"""
Session store — pluggable backend for agent state persistence.
Default: in-memory dict (for development).
Production: Redis (for scale and persistence).
"""
import json
import uuid
from abc import ABC, abstractmethod
from typing import Optional
from .agents import AgentState


class SessionStore(ABC):
    """Abstract base for session storage."""

    @abstractmethod
    async def get(self, session_id: str) -> Optional[AgentState]:
        """Retrieve session by ID."""
        pass

    @abstractmethod
    async def save(self, session: AgentState, ttl_seconds: int = 86400) -> None:
        """Save session (with optional TTL)."""
        pass

    @abstractmethod
    async def delete(self, session_id: str) -> None:
        """Delete session."""
        pass

    @abstractmethod
    async def exists(self, session_id: str) -> bool:
        """Check if session exists."""
        pass


class InMemorySessionStore(SessionStore):
    """Development: in-memory storage (not distributed)."""

    def __init__(self):
        self._store: dict[str, AgentState] = {}

    async def get(self, session_id: str) -> Optional[AgentState]:
        return self._store.get(session_id)

    async def save(self, session: AgentState, ttl_seconds: int = 86400) -> None:
        self._store[session.session_id] = session

    async def delete(self, session_id: str) -> None:
        if session_id in self._store:
            del self._store[session_id]

    async def exists(self, session_id: str) -> bool:
        return session_id in self._store


class RedisSessionStore(SessionStore):
    """Production: Redis-backed storage (distributed, persistent)."""

    def __init__(self, redis_url: str = "redis://localhost:6379"):
        """
        Initialize Redis session store.
        Args:
            redis_url: Redis connection string (e.g., "redis://localhost:6379/0")
        """
        import redis.asyncio as redis

        self.redis_url = redis_url
        self.redis: Optional[redis.Redis] = None
        self.key_prefix = "agent:session:"

    async def connect(self) -> None:
        """Establish Redis connection."""
        import redis.asyncio as redis

        self.redis = await redis.from_url(self.redis_url, decode_responses=True)
        # Test connection
        await self.redis.ping()
        print(f"✅ Connected to Redis: {self.redis_url}")

    async def disconnect(self) -> None:
        """Close Redis connection."""
        if self.redis:
            await self.redis.close()

    async def get(self, session_id: str) -> Optional[AgentState]:
        """Retrieve session from Redis."""
        if not self.redis:
            await self.connect()

        json_str = await self.redis.get(f"{self.key_prefix}{session_id}")
        if not json_str:
            return None

        # Deserialize from JSON
        data = json.loads(json_str)
        state = AgentState(session_id)
        state.messages = data.get("messages", [])
        state.requires_approval = data.get("requires_approval", False)
        state.pending_action = data.get("pending_action")
        state.step_count = data.get("step_count", 0)
        state.created_at = data.get("created_at")
        return state

    async def save(self, session: AgentState, ttl_seconds: int = 86400) -> None:
        """Save session to Redis with TTL."""
        if not self.redis:
            await self.connect()

        json_str = json.dumps(session.to_dict())
        await self.redis.setex(f"{self.key_prefix}{session.session_id}", ttl_seconds, json_str)

    async def delete(self, session_id: str) -> None:
        """Delete session from Redis."""
        if not self.redis:
            await self.connect()

        await self.redis.delete(f"{self.key_prefix}{session_id}")

    async def exists(self, session_id: str) -> bool:
        """Check if session exists in Redis."""
        if not self.redis:
            await self.connect()

        return await self.redis.exists(f"{self.key_prefix}{session_id}") > 0


# Global session store instance
_session_store: Optional[SessionStore] = None


def get_session_store() -> SessionStore:
    """Get the current session store (lazy initialize)."""
    global _session_store
    if _session_store is None:
        # Auto-detect: try Redis, fall back to in-memory
        from .config import settings

        try:
            redis_url = getattr(settings, "redis_url", None)
            if redis_url:
                _session_store = RedisSessionStore(redis_url)
                print(f"📦 Using Redis session store: {redis_url}")
            else:
                _session_store = InMemorySessionStore()
                print("⚠️  Using in-memory session store (data lost on restart)")
        except Exception as e:
            print(f"⚠️  Redis connection failed, falling back to in-memory: {e}")
            _session_store = InMemorySessionStore()

    return _session_store


async def get_or_create_session(session_id: str | None = None) -> AgentState:
    """Get existing session or create new one."""
    store = get_session_store()

    if session_id is None:
        session_id = str(uuid.uuid4())

    # Try to load from store
    session = await store.get(session_id)
    if session:
        return session

    # Create new session
    session = AgentState(session_id)
    await store.save(session)
    return session


async def save_session(session: AgentState) -> None:
    """Explicitly save session to store."""
    store = get_session_store()
    await store.save(session, ttl_seconds=86400)  # 24 hour TTL

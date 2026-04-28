"""
MCPAuthManager — OAuth2 Client Credentials flow for the MCP service account.

Authenticates directly against Keycloak using client_id + client_secret.
Caches the access token and refreshes it automatically 60 seconds before expiry.

Usage:
    from .auth import MCPAuthManager
    manager = MCPAuthManager(keycloak_url, realm, client_id, client_secret)
    token = await manager.get_token()
    headers = {"Authorization": f"Bearer {token}"}
"""

import logging
import time
from typing import Optional

import httpx

logger = logging.getLogger(__name__)


class MCPAuthManager:
    """OAuth2 Client Credentials manager — fetches and caches Keycloak service-account tokens."""

    def __init__(self, keycloak_url: str, realm: str, client_id: str, client_secret: str) -> None:
        self._token_url = f"{keycloak_url}/realms/{realm}/protocol/openid-connect/token"
        self._client_id = client_id
        self._client_secret = client_secret
        self._token: Optional[str] = None
        self._expires_at: float = 0.0

    async def get_token(self) -> str:
        """Returns a valid access token, refreshing automatically if near expiry."""
        if self._token is None or time.time() >= (self._expires_at - 60):
            await self._refresh()
        return self._token  # type: ignore[return-value]

    def invalidate(self) -> None:
        """Force token refresh on the next get_token() call (use after a 401 response)."""
        self._token = None
        self._expires_at = 0.0

    async def _refresh(self) -> None:
        """Fetch a new token from Keycloak using the Client Credentials grant."""
        if not self._client_id or not self._client_secret:
            raise RuntimeError(
                "MCP_CLIENT_ID and MCP_CLIENT_SECRET environment variables must be set"
            )
        async with httpx.AsyncClient() as client:
            resp = await client.post(
                self._token_url,
                # Client Credentials grant requires form-encoded body (not JSON)
                data={
                    "grant_type": "client_credentials",
                    "client_id": self._client_id,
                    "client_secret": self._client_secret,
                },
                timeout=10.0,
            )
            resp.raise_for_status()
            body = resp.json()
            self._token = body["access_token"]
            # expires_in is in seconds; refresh 60s before actual expiry
            expires_in = body.get("expires_in", 900)
            self._expires_at = time.time() + expires_in
            logger.info(
                "MCP service account token refreshed via Client Credentials (expires in %ds)",
                expires_in,
            )

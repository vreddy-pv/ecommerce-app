# Frontend + Redis Integration Guide

**Complete setup for wiring the React frontend to the agent and scaling to Redis.**

---

## Part 1: Frontend Integration (React)

### Step 1 — Files Added

```
frontend/src/
├── api/agent.ts                    # API client for agent endpoints
├── context/AgentContext.tsx        # React Context for chat state
├── components/AgentChat.tsx        # Chat UI component
├── components/AgentChat.module.css # Styles
└── pages/AgentPage.tsx             # Page wrapper
```

### Step 2 — Add Agent Page to Router

Edit `frontend/src/App.tsx`:

```tsx
import { AgentPage } from './pages/AgentPage'

// Inside your routes (React Router or similar):
<Route path="/agent" element={<AgentPage />} />
// or
<Route path="/ai-assistant" element={<AgentPage />} />
```

### Step 3 — Add Navigation Link

Add a link in your navbar to `/agent`:

```tsx
// In Navbar.tsx or similar:
<Link to="/agent">🤖 AI Agent</Link>
```

### Step 4 — Install Dependencies

```bash
# If not already installed:
npm install axios
```

Your React app already has axios configured (in `src/api/client.ts` with Keycloak auth).

### Step 5 — Test

```bash
# Terminal 1: Backend services
docker-compose up -d

# Terminal 2: Frontend
cd frontend
npm start
# Visit http://localhost:3000 → click "AI Agent" navbar link
```

---

## Part 2: Redis Persistence (Production Scale)

### Why Redis?

- **In-memory:** Sessions load fast (< 50ms)
- **Distributed:** Share state across multiple workers
- **Persistence:** Data survives app restart
- **TTL:** Auto-expire old sessions (24h default)
- **Scale:** Support 100+ concurrent users

### Step 1 — Update Configuration

Edit `.env`:

```bash
# Enable Redis (add this line)
REDIS_URL=redis://redis:6379/0

# Or for production:
REDIS_URL=redis://redis-prod.yourhost.com:6379/0
```

### Step 2 — Update docker-compose.yml

Ensure Redis service is defined:

```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  mcp-server:
    # ... existing config ...
    depends_on:
      redis:
        condition: service_healthy
    environment:
      - REDIS_URL=redis://redis:6379/0

volumes:
  redis-data:
```

### Step 3 — Verify Redis Connection

After starting services:

```bash
# Test Redis is reachable
docker exec ecommerce-redis redis-cli ping
# Expected output: PONG

# Check agent connected
docker logs ecommerce-mcp-server | grep -i "redis\|connected"
# Expected: "Connected to Redis: redis://redis:6379/0"
```

### Step 4 — Test Session Persistence

```bash
# Start conversation
curl -X POST http://localhost:8090/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Check order ORD-123"
  }'

# Save the session_id from response, then:
# Stop and restart the agent
docker restart ecommerce-mcp-server

# Session still exists in Redis!
curl http://localhost:8090/api/ai/session/SESSION-ID
# Returns: {"messages": [...], ...}
```

---

## Architecture: Frontend → Backend → Redis

```
Browser (React)
    │
    ├─ GET /agent (fetch AgentPage)
    │
    └─ POST http://localhost:3000/api/ai/chat
       (forwarded by React via axios client)
           │
           ▼
    API Gateway (:8080)
       │ (validates JWT)
       │
       ▼
    MCP Server (:8090) — Agent FastAPI
       │
       ├─ Claude API (reasoning, tool scheduling)
       │
       ├─ Agent Tools
       │   ├─ get_order_details → Order Service
       │   ├─ check_fraud_score → Order Service
       │   ├─ cancel_order → Order Service (approval gate)
       │   └─ ...
       │
       └─ Session Store
           ├─ In-Memory (dev): Python dict
           └─ Redis (prod): redis://redis:6379/0
```

---

## Frontend Component Reference

### AgentContext (State Management)

```tsx
import { useAgent } from '../context/AgentContext'

const MyComponent = () => {
  const { 
    sessionId,           // Current session ID
    messages,            // Array of {role, content, timestamp}
    isLoading,           // True while waiting for agent
    requiresApproval,    // True when action needs confirmation
    pendingAction,       // {tool, input, tool_call_id}
    error,               // Error message or null
    sendMessage,         // async (text) => void
    approveAction,       // async (approve, reason?) => void
    clearError,          // () => void
    resetSession,        // () => void
  } = useAgent()
}
```

### AgentChat Component

Renders the entire chat UI (messages, input, approval dialog).

Usage:

```tsx
import { AgentProvider } from '../context/AgentContext'
import { AgentChat } from '../components/AgentChat'

export default () => (
  <AgentProvider>
    <AgentChat />
  </AgentProvider>
)
```

### API Functions

```tsx
import { chat, approveAction, getSession, deleteSession } from '../api/agent'

// Send message
const response = await chat({
  session_id: null,  // null = new session
  message: "Check fraud"
})
// Returns: { session_id, response, requires_approval, pending_action, ... }

// Approve pending action
const result = await approveAction({
  session_id: "uuid",
  approve: true,
  reason: "Fraud confirmed"
})

// Inspect session state
const state = await getSession(sessionId)
// Returns: { session_id, messages, requires_approval, ... }

// Clean up
await deleteSession(sessionId)
```

---

## Session Lifecycle

### New Session (no session_id provided)

```
1. User sends message → Agent generates session_id (UUID)
2. Message stored in Redis with key: agent:session:{id}
3. User can reuse same session_id for follow-up messages
4. Session auto-expires after 24 hours (TTL)
```

### Approval Flow

```
1. User sends message with sensitive operation request
2. Agent detects sensitive tool (cancel_order, update_inventory)
3. Returns: requires_approval=true, pending_action={...}
4. Frontend shows approval dialog
5. User approves/rejects → POST /chat/approve
6. Backend executes action (or rejects)
7. Agent resumes reasoning with result
8. Returns final response
9. Session saved to Redis
```

### Session Expiration

```
TTL = 24 hours from last update

// Extend TTL by sending a message
await chat({ session_id: "old-id", message: "..." })
// TTL resets to 24h from now

// Or explicitly persist
// (backend auto-saves on every /chat or /chat/approve call)
```

---

## Troubleshooting

### Frontend Can't Connect to Agent

**Error:** `POST http://localhost:8090/api/ai/chat 500 (Internal Server Error)`

**Fix:**
1. Verify agent is running: `curl http://localhost:8090/health`
2. Check ANTHROPIC_API_KEY is set: `echo $ANTHROPIC_API_KEY`
3. Check API Gateway is running: `curl http://localhost:8080/actuator/health`
4. Check proxy in frontend (if any) — ensure `/api/ai` routes to `:8090`

### Sessions Not Persisting

**Error:** Session lost after app restart

**Fix:**
1. Verify Redis is running: `docker ps | grep redis`
2. Check REDIS_URL in `.env`: `grep REDIS_URL .env`
3. Check agent logs: `docker logs ecommerce-mcp-server | grep redis`
4. Test Redis directly:
   ```bash
   docker exec ecommerce-redis redis-cli
   > keys agent:session:*
   # Should see session keys
   ```

### Approval Dialog Not Showing

**Error:** `requires_approval=true` but no dialog appears

**Fix:**
1. Check AgentContext is wrapping AgentChat:
   ```tsx
   <AgentProvider>
     <AgentChat />
   </AgentProvider>
   ```
2. Verify useAgent hook is imported:
   ```tsx
   import { useAgent } from '../context/AgentContext'
   ```
3. Check browser console for errors: `F12 → Console tab`

### Agent Returns Generic Responses

**Error:** Agent not using tools or always says "I can't help with that"

**Fix:**
1. Check Claude can see the tools — test via curl:
   ```bash
   curl -X POST http://localhost:8090/api/ai/chat \
     -d '{"message": "What tools are available?"}'
   ```
2. Check Keycloak token — agent needs admin role:
   ```bash
   docker exec ecommerce-keycloak /opt/keycloak/bin/kcadm.sh \
     get users -r ecommerce -q username=admin
   ```
3. Check API Gateway can reach Order Service:
   ```bash
   curl http://localhost:8080/api/orders/admin/summary
   # Should return order summary (not 404)
   ```

---

## Performance Tuning

### For Single Worker (dev)

```bash
# Just run as-is
uvicorn app.main:app --port 8090
```

### For Scale (prod)

```bash
# 4 async workers
uvicorn app.main:app --workers 4 --port 8090 --loop uvloop

# Or in docker-compose.yml:
mcp-server:
  command: uvicorn app.main:app --host 0.0.0.0 --port 8090 --workers 4
```

### Monitor Session Count

```bash
# Check Redis memory usage
docker exec ecommerce-redis redis-cli info memory

# Count active sessions
docker exec ecommerce-redis redis-cli keys "agent:session:*" | wc -l

# Check oldest session TTL
docker exec ecommerce-redis redis-cli --scan --match "agent:session:*" | \
  xargs -I {} redis-cli ttl {} | sort -n | head -1
```

---

## Cost Estimation

### Claude API

- **~200 tokens per chat turn** (depends on order complexity)
- **Claude 3.5 Sonnet:** $3/M input, $15/M output tokens
- **100 chat turns/day:** ~20K tokens/day ≈ $0.07/day ≈ $2/month

### Redis

- **~1KB per session** (messages + state)
- **1000 concurrent sessions:** ~1MB RAM
- **AWS ElastiCache (1GB, dev):** ~$10/month
- **Self-hosted Redis in Docker:** free (included in your compose stack)

### Storage

- **PostgreSQL:** Unchanged (order data)
- **Agent doesn't persist anything to DB** (only session state in Redis)

---

## Next Steps

1. **Wire frontend** (10 min)
   - Copy agent files to frontend/src
   - Add route in App.tsx
   - Add navbar link

2. **Test locally** (5 min)
   - npm start → visit /agent
   - Send a test message

3. **Set up Redis** (5 min)
   - Add REDIS_URL to .env
   - Restart mcp-server

4. **Monitor in production** (ongoing)
   - Watch Redis memory
   - Track Claude API costs
   - Review fraud detection accuracy

---

## Support

- **Frontend issues:** Check browser console, React DevTools
- **Backend issues:** `docker logs ecommerce-mcp-server`
- **Redis issues:** `docker logs ecommerce-redis`
- **Agent reasoning issues:** Increase Claude context, add examples to system prompt

See `AGENT_README.md` for API details and extending with custom tools.

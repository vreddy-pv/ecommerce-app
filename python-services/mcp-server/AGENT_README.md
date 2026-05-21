# E-Commerce Autonomous AI Agent

**Production-grade multi-step autonomous agent for e-commerce operations.**

Analyzes orders, detects fraud, checks inventory, and executes actions across your 7 microservices with human-in-the-loop approval gates for sensitive operations.

---

## What's Inside

### Core Components (7 files)

1. **`agents.py`** — Agent reasoning loop
   - Multi-turn conversation state management
   - Tool scheduling and execution
   - Approval gate detection
   - Session persistence (in-memory; plug Redis for scale)

2. **`agent_tools.py`** — 7 tools wrapping your microservices
   - `get_order_details()` — Order analysis
   - `check_fraud_score()` — Fraud detection (0-100 scale)
   - `check_inventory_levels()` — Stock checks
   - `get_recent_orders()` — Customer history
   - `flag_for_review()` — Safe flagging (no approval needed)
   - `cancel_order()` — Sensitive (requires user approval)
   - `update_inventory()` — Sensitive (requires user approval)

3. **`chat.py`** — FastAPI endpoints
   - `POST /chat` — Normal turns (user message → agent reasoning → response)
   - `POST /chat/approve` — Approval flow (user confirms sensitive operation)
   - `GET /session/{id}` — Debug/introspection

4. **`guardrails.py`** — Input/output safety
   - **Input:** Prompt injection blocking (15 patterns), PII masking (emails, cards, Aadhaar, PAN, SSN, phones), message length enforcement
   - **Output:** Cost/margin data stripping, policy disclaimers, accidental PII leakage prevention

5. **`config.py`** — Environment configuration
   - API Gateway URL
   - Keycloak credentials
   - Anthropic API key

6. **`requirements.txt`** — Python dependencies
   - FastAPI, Uvicorn, httpx (async HTTP)
   - anthropic (Claude SDK)
   - pydantic-settings (config management)
   - (langgraph removed; using direct Claude API instead)

7. **`AGENT_README.md`** — This file

---

## Quick Start (3 steps)

### 1. Configure

```bash
cd python-services/mcp-server

# Copy environment template
cp .env.example .env

# Edit .env with your values:
# - ANTHROPIC_API_KEY=sk-...
# - KEYCLOAK_URL=http://keycloak:8080
# - GATEWAY_URL=http://api-gateway:8080
# - MCP_CLIENT_SECRET=changeme-mcp-secret (from your Keycloak config)
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Run

**Development:**
```bash
uvicorn app.main:app --reload --port 8090
```

**Docker (alongside existing stack):**
```bash
docker-compose up -d mcp-server
```

The agent runs on `http://localhost:8090/api/ai/chat`.

---

## API Endpoints

### `POST /api/ai/chat`

User message → agent reasoning → response.

**Request:**
```json
{
  "session_id": "uuid-or-null",
  "message": "Check if order ORD-123 is fraudulent"
}
```

**Response:**
```json
{
  "session_id": "generated-if-null",
  "response": "I'll check the fraud score for order ORD-123... [result]",
  "requires_approval": false,
  "pending_action": null,
  "step_count": 2,
  "messages_in_session": 3
}
```

### `POST /api/ai/chat/approve`

User approves/rejects sensitive operation, agent resumes.

**Request (approve):**
```json
{
  "session_id": "xxx",
  "approve": true,
  "reason": "Fraud confirmed. User contacted."
}
```

**Request (reject):**
```json
{
  "session_id": "xxx",
  "approve": false,
  "reason": "Not a fraud case; customer is legitimate."
}
```

**Response:** Same as `/chat` (new reasoning after approval/rejection).

### `GET /api/ai/session/{session_id}`

Inspect session state (for debugging).

```json
{
  "session_id": "uuid",
  "messages": [{...}],
  "requires_approval": true,
  "pending_action": {
    "tool": "cancel_order",
    "input": {"order_id": "ORD-123", "reason": "FRAUD_SUSPECTED"},
    "tool_call_id": "tool_abc123"
  },
  "step_count": 3,
  "created_at": "2025-05-16T10:30:00Z"
}
```

### `DELETE /api/ai/session/{session_id}`

Clean up a session.

---

## Approval Gate Flow (for Sensitive Operations)

When the agent needs to cancel an order or update inventory:

1. Agent reasons and schedules the operation
2. Hits the approval gate → returns `requires_approval: true` + `pending_action`
3. Frontend shows confirmation dialog to user
4. User clicks Approve/Reject → UI calls `POST /chat/approve`
5. Backend executes the operation (or rejects it)
6. Agent resumes reasoning with the result
7. Returns final response

**No sensitive operation is ever executed without user confirmation.**

---

## Example Conversation

```
User: "Check order ORD-456 for fraud, and flag it for review if risky."

Agent (step 1):
- Fetches order details
- Runs fraud score check
- Sees score 78 (HIGH RISK)

Agent (step 2):
- Flags order for review with reason "HIGH_FRAUD_SCORE"
- Returns: "Order ORD-456 has fraud score 78 (HIGH RISK). I've flagged it for manual review."
- requires_approval: false (flagging is safe)

User: "Actually, cancel that order—we confirmed it's fraud."

Frontend: Shows dialog "Agent wants to cancel ORD-456. Approve?"
User: Clicks Approve

Agent (resume):
- Executes cancel_order("ORD-456", reason="FRAUD_SUSPECTED")
- Returns: "Order cancelled. Notification sent to customer."
```

---

## Fraud Detection Rules

- **HIGH RISK:** score > 70 → Agent flags for review before other actions
- **MEDIUM RISK:** score 30-70 → Agent proceeds with caution, explains reasoning
- **LOW RISK:** score < 30 → Agent uses order data freely

The fraud score comes from your Order Service's ML/rule engine.

---

## Guardrails in Action

### Input Example (PII masking)

```
User: "The customer's email is john.doe@example.com and card is 4111-1111-1111-1111"

Sanitized: "The customer's email is [EMAIL_REDACTED] and card is [CREDIT_CARD_REDACTED]"
```

### Output Example (data stripping)

```
Agent: "Order total: $99.99, our cost: $45.00, margin: 55%"

Sanitized: "Order total: $99.99 [COST_REDACTED] [MARGIN_REDACTED]

---
*Note: This response was generated by an AI agent. All sensitive operations require human review and approval. For concerns, contact support.*"
```

---

## Extending the Agent

### Add a New Tool

1. Add function to `agent_tools.py`:
```python
async def my_new_tool(param1: str) -> dict:
    """Tool description for Claude."""
    return await _post(f"{settings.gateway_url}/api/path", {"key": param1})
```

2. Add schema to `_get_tools_schema()` in `agents.py`:
```python
{
    "name": "my_new_tool",
    "description": "Tool description",
    "input_schema": {
        "type": "object",
        "properties": {"param1": {"type": "string"}},
        "required": ["param1"],
    },
}
```

3. Claude will auto-discover and use it.

### Add Custom Guardrails

Edit `guardrails.py`:
- Add patterns to `InputGuard.INJECTION_PATTERNS` (prompt injection)
- Add patterns to `InputGuard.PII_PATTERNS` (PII masking)
- Add patterns to `OutputGuard.SENSITIVE_PATTERNS` (output stripping)

### Use Redis for Session Persistence (production)

Replace in-memory `_sessions` dict with Redis:
```python
import redis

redis_client = redis.Redis(host="localhost", port=6379, decode_responses=True)

def get_or_create_session(session_id=None):
    if session_id is None:
        session_id = str(uuid.uuid4())
    
    # Try to load from Redis
    saved = redis_client.get(f"agent:session:{session_id}")
    if saved:
        return AgentState.from_json(saved)
    
    # Create new
    session = AgentState(session_id)
    redis_client.set(f"agent:session:{session_id}", session.to_json(), ex=86400)  # 24h TTL
    return session
```

---

## Testing

```bash
# Unit tests (mocks the microservices)
cd python-services/mcp-server
pytest tests/

# Integration test (requires live API Gateway)
curl -X POST http://localhost:8090/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is the system health?"
  }'
```

---

## Monitoring & Debugging

### Check Agent Logs
```bash
docker logs ecommerce-mcp-server | grep "agent\|error"
```

### Inspect a Session
```bash
curl http://localhost:8090/api/ai/session/SESSION-ID
```

### Reset a Session
```bash
curl -X DELETE http://localhost:8090/api/ai/session/SESSION-ID
```

### Trace Tool Calls
Add logging to `agent_tools.py`:
```python
import logging
logger = logging.getLogger(__name__)

async def my_tool(...):
    logger.info(f"Calling tool with {input}")
    ...
```

---

## Performance Notes

- **Response time:** ~2-5 sec (including Claude API, tool execution, and guard processing)
- **Concurrent sessions:** Limited by Python async worker pool (default: 10 concurrent)
- **Session memory:** ~1KB per session; 1000 sessions ≈ 1MB RAM
- **Token usage:** ~150-300 tokens per turn (depends on order complexity)

For scale (>100 concurrent sessions):
- Use async workers: `uvicorn app.main:app --workers 4`
- Replace in-memory sessions with Redis
- Add API rate limiting

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `Anthropic API Key not set` | Set `ANTHROPIC_API_KEY` in `.env` and restart |
| `Tool returns 403 Unauthorized` | Verify `MCP_CLIENT_SECRET` matches Keycloak config |
| `Agent gives generic responses` | Check if API Gateway is reachable; test `/api/orders/admin/summary` |
| `Approval gate never triggers` | Verify tool name is in `["cancel_order", "update_inventory"]` |
| `PII not masked` | Add pattern to `InputGuard.PII_PATTERNS` and test with regex |

---

## Architecture Diagram

```
Frontend (React)
    │
    ├─ POST /api/ai/chat
    │   └─ Sanitize input → Run agent loop → Sanitize output
    │       │
    │       ├─ Call Claude API (tool_use)
    │       ├─ Execute tools (auth via Keycloak)
    │       │   ├─ get_order_details → API Gateway → Order Service
    │       │   ├─ check_fraud_score → API Gateway → Order Service (ML)
    │       │   ├─ check_inventory_levels → API Gateway → Inventory Service
    │       │   └─ cancel_order / update_inventory → APPROVAL GATE
    │       │
    │       └─ Return response (+ approval_required flag)
    │
    └─ POST /api/ai/chat/approve
        └─ Execute pending action (if approved)
            └─ Resume agent reasoning
```

---

## License

Same as parent project. Built for e-commerce.

---

## Support

For issues or feature requests, check the [microservices README](../../../CLAUDE.md).

# E-Commerce Microservices — Developer Guide

## Architecture at a Glance

```
Browser/Mobile
      │
      ▼
[Keycloak :8180]  ←── SSO / Authorization Code + PKCE (frontend users)
      │                   Client Credentials (MCP server service account)
      ▼
[Nginx HTTP Cache :80]
      │
      ▼
[API Gateway :8080]  ←── validates Keycloak JWT via JWKS endpoint
      │                   injects X-User-Id, X-User-Name, X-User-Role headers
      ▼
[Aggregator :8081]  ←─── publishes events to RabbitMQ (analytics)
      │
      ┌───────────────────────────────────────────────────────────┐
      ▼           ▼             ▼            ▼            ▼        ▼
[User      [Catalog     [Inventory  [Order     [Order      [Notif.
 :8082]     :8083]       :8084]      :8085]     Processing  :8087]
              │                                  :8086]
         [Object Cache]                    ↕ heartbeats
         [Session Cache]
              │
              ▼
      [Analytics :8086]  ←─── consumes RabbitMQ events (search, views, feedback)
              │
              ▼
      [analytics_db]       ←─── event persistence

[RabbitMQ :5672]          ←─── message broker (event-driven pipeline)
[MCP Server :8090] (Python) ←── Claude AI integration
```

**Auth Architecture (Keycloak SSO)**:
- Frontend users → Authorization Code + PKCE flow → Keycloak login page → JWT
- MCP Server → Client Credentials grant → Keycloak → JWT (machine-to-machine, no login prompt)
- API Gateway validates all JWTs via Keycloak JWKS endpoint (no static keys needed)
- Downstream services trust `X-User-*` headers injected by the gateway

**Monitoring (two patterns)**:
- Hierarchical health checks: Gateway polls all services via `/actuator/health`
- Peer-to-peer heartbeats: Inventory ↔ Order Processing ↔ Order Service ping each other every 30s

**Caching (three layers)**:
- Layer 1 — Nginx HTTP cache (catalog products, categories)
- Layer 2 — Redis DB 0: Session cache (user sessions, owned by Aggregator)
- Layer 3 — Redis DB 1: Object cache (products, inventory, owned by Catalog/Inventory)

---

## Quick Start

```bash
# 1. Start all services (Keycloak realm is auto-imported on first start)
docker-compose up -d

# 2. Wait for Keycloak to be ready (~60-90s on first start)
docker-compose logs -f keycloak | grep "started in"

# 3. Verify all containers are healthy
docker-compose ps

# 4. Open the app
# Frontend:          http://localhost:3000
# Keycloak Admin UI: http://localhost:8180  (admin / admin)
# API Gateway:       http://localhost:8080
# Eureka Dashboard:  http://localhost:8761
```

> **First start note:** Keycloak imports `infrastructure/keycloak/ecommerce-realm.json` automatically.
> On subsequent starts it skips re-import (idempotent). To force re-import: `docker-compose down -v` then `up`.

---

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| Frontend (React) | 3000 | http://localhost:3000 |
| Nginx (HTTP Cache) | 80 | http://localhost |
| API Gateway | 8080 | http://localhost:8080 |
| Aggregator Service | 8081 | http://localhost:8081 |
| User Service | 8082 | http://localhost:8082 |
| Catalog Service | 8083 | http://localhost:8083 |
| Inventory Service | 8084 | http://localhost:8084 |
| Order Service | 8085 | http://localhost:8085 |
| Order Processing | 8086 | http://localhost:8086 |
| Analytics Service | 8086 | http://localhost:8086 |
| Notification Service | 8087 | http://localhost:8087 |
| MCP Server | 8090 | http://localhost:8090 |
| **Keycloak** | **8180** | **http://localhost:8180** |
| Eureka Dashboard | 8761 | http://localhost:8761 |
| RabbitMQ Management | 15672 | http://localhost:15672 |
| MailHog (dev email) | 8025 | http://localhost:8025 |
| PostgreSQL | 5432 | localhost:5432 |
| Redis | 6379 | localhost:6379 |

---

## Default Dev Credentials

| Resource | Username | Password | Notes |
|----------|----------|----------|-------|
| **Keycloak Master Admin** | `admin` | `admin` | Keycloak Admin UI at :8180 |
| **App Admin** (ecommerce realm) | `admin` | `admin123` | Login via frontend at :3000 |
| RabbitMQ Management | `admin` | `changeme` | http://localhost:15672 |
| PostgreSQL | `app_user` | `changeme` | All service databases |
| Redis | — | no password | |

> **Important:** The app admin (`admin / admin123`) is seeded automatically from `infrastructure/keycloak/ecommerce-realm.json`.
> No manual DB updates needed — Keycloak is the source of truth for all user accounts.

---

## Keycloak SSO Configuration

### Realm: `ecommerce`
Auto-imported from `infrastructure/keycloak/ecommerce-realm.json` on first start.

### Clients
| Client ID | Type | Flow | Used by |
|-----------|------|------|---------|
| `ecommerce-frontend` | Public | Authorization Code + PKCE | React frontend |
| `mcp-server` | Confidential | Client Credentials | MCP tools (Claude AI) |

### Roles
| Role | Description |
|------|-------------|
| `user` | Regular customer — default for all new registrations |
| `admin` | Store administrator — can access `/api/*/admin/**` endpoints |

### Add Google Sign-In (optional, no code needed)
1. Open Keycloak Admin UI → http://localhost:8180
2. Select realm: **ecommerce**
3. Left menu → **Identity Providers** → **Add provider** → **Google**
4. Paste your Google Client ID & Secret from Google Cloud Console
5. Save — Google login appears on the Keycloak login page automatically

### Get a token manually (for API testing)
```bash
# App admin token
TOKEN=$(curl -s -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -d "grant_type=password&client_id=ecommerce-frontend&username=admin&password=admin123" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# Use it
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/orders/admin/summary | python3 -m json.tool

# MCP service account token (Client Credentials)
MCP_TOKEN=$(curl -s -X POST http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -d "grant_type=client_credentials&client_id=mcp-server&client_secret=changeme-mcp-secret" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

---

## MCP Server (Claude AI Integration)

The MCP server exposes 4 tools to Claude via the Model Context Protocol:

| Tool | Description |
|------|-------------|
| `get_sales_summary` | Aggregated revenue/orders for `today`, `7d`, or `30d` |
| `get_low_inventory_alerts` | Products at or below reorder threshold |
| `search_orders` | Search orders by status, ID, or keyword |
| `get_system_health` | Composite health across all services |

### Authentication flow (automatic, no user action needed)
```
Claude calls MCP tool
  → MCP server: POST /realms/ecommerce/protocol/openid-connect/token
                grant_type=client_credentials
                client_id=mcp-server + client_secret
  ← Keycloak: JWT with admin role
  → API Gateway: Bearer <JWT>  (token cached, refreshed ~15 min)
  ← Order/Inventory data returned to Claude
```

### Claude Desktop configuration
File: `%APPDATA%\Claude\claude_desktop_config.json`
```json
"ecommerce": {
  "command": "C:\\path\\to\\mcp-server\\.venv\\Scripts\\python.exe",
  "args": ["-u", "-m", "app.mcp_server"],
  "env": {
    "PYTHONUNBUFFERED": "1",
    "PYTHONPATH": "C:\\path\\to\\mcp-server",
    "GATEWAY_URL": "http://localhost:8080",
    "KEYCLOAK_URL": "http://localhost:8180",
    "KEYCLOAK_REALM": "ecommerce",
    "MCP_CLIENT_ID": "mcp-server",
    "MCP_CLIENT_SECRET": "changeme-mcp-secret"
  }
}
```

### Claude Code configuration
File: `.mcp.json` (at project root — already committed)
```bash
cd C:\Veera\AI\agents\microservices
claude    # .mcp.json is auto-loaded; type /mcp to verify
```

---

## Analytics Pipeline (Event-Driven)

The analytics service captures user activity events (searches, product views, recommendation feedback) via an asynchronous, fire-and-forget RabbitMQ pipeline.

### Enable/Disable Analytics
```yaml
# In docker-compose.yml or runtime environment:
ANALYTICS_ENABLED: "true"   # Default; set to "false" to disable entirely
```
When disabled, analytics events are silently dropped (no impact on order processing or core functionality).

### Event Flow
```
Frontend/UI
    │
    ├─ POST /api/analytics/search-event           (Aggregator :8081)
    │   {searchQuery, resultCount, clickedProductId}
    │
    ├─ POST /api/analytics/product-view-event     (Aggregator :8081)
    │   {productId, sessionId, durationSeconds, source}
    │
    └─ POST /api/recommendations/feedback         (Aggregator :8081)
       {productId, recommendationId, action, orderId}
           │
           ▼
    RabbitMQ (user-activity.exchange)
           │
    ┌──────┼──────┐
    ▼      ▼      ▼
   [search] [view] [feedback] queues
    │      │      │
    └──────┼──────┘
           ▼
    Analytics Service (:8086)
    @RabbitListener consumers
           │
           ▼
    analytics_db (PostgreSQL)
    ├─ user_search_events
    ├─ product_view_events
    ├─ recommendation_feedback_events
    ├─ product_attributes
    └─ recommendation_experiments
```

### Analytics Endpoints

**POST /api/analytics/search-event** (Aggregator)
```bash
curl -X POST http://localhost:8081/api/analytics/search-event \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-123" \
  -d '{
    "searchQuery": "summer wedding dress",
    "resultCount": 12,
    "clickedProductId": null  # optional
  }'
# Returns: 202 ACCEPTED
```

**POST /api/analytics/product-view-event** (Aggregator)
```bash
curl -X POST http://localhost:8081/api/analytics/product-view-event \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-456" \
  -d '{
    "productId": 42,
    "sessionId": "sess-xyz",
    "durationSeconds": 45,
    "source": "search"  # or "category_browse", "recommendation", "direct"
  }'
# Returns: 202 ACCEPTED
```

### Querying Analytics Data

```bash
# Connect to analytics_db
docker exec -e PGPASSWORD=changeme ecommerce-postgres psql -U app_user -d analytics_db

# Recent search events
SELECT user_id, search_query, result_count, clicked_product_id 
FROM user_search_events 
ORDER BY created_at DESC LIMIT 10;

# Product view sessions
SELECT user_id, product_id, session_id, duration_seconds, source
FROM product_view_events
ORDER BY created_at DESC LIMIT 10;

# Event counts by date
SELECT DATE(created_at) as date, COUNT(*) as event_count
FROM user_search_events
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

### Debugging Analytics Issues

**No events appearing in database?**
```bash
# 1. Check RabbitMQ queue depth
curl -s -u admin:changeme http://localhost:15672/api/queues/%2F \
  | jq '.[] | {name, messages}'

# 2. Check Analytics service logs
docker logs ecommerce-analytics | grep -i "error\|listener\|search"

# 3. Verify exchange/bindings exist
curl -s -u admin:changeme http://localhost:15672/api/exchanges/%2F \
  | jq '.[] | select(.name | contains("activity"))'
```

**Analytics disabled but events still publishing?**
The `ANALYTICS_ENABLED` flag only affects the Aggregator service. If true, events are published to RabbitMQ. If false, the `AnalyticsEventService` bean is not instantiated, so publishing is skipped silently.

---

## Running Tests

```bash
# All Java services (unit tests, H2 in-memory)
cd backend-services && ./mvnw test

# Single service
cd backend-services && ./mvnw test -pl catalog-service

# With Testcontainers integration tests (requires Docker)
cd backend-services && ./mvnw verify

# Coverage report (generated in target/site/jacoco/index.html)
cd backend-services && ./mvnw verify jacoco:report

# Python services
cd python-services/notification-service && pytest tests/ -v --cov
cd python-services/mcp-server && pytest tests/ -v --cov

# Frontend
cd frontend && npm test -- --coverage
```

---

## Docker Operations

```bash
# Start all services
docker-compose up -d

# Rebuild a single service after code change
docker-compose up -d --build catalog-service

# View logs (follow)
docker-compose logs -f catalog-service

# View last 100 lines
docker-compose logs --tail=100 api-gateway

# Stop all services (data preserved)
docker-compose down

# Stop and remove all data volumes (full reset — also re-imports Keycloak realm)
docker-compose down -v

# Check service health
docker-compose ps

# Restart a single unhealthy service
docker-compose restart keycloak
```

---

## Common Issues & Fixes

### Can't log in to the app (admin/admin123)
```bash
# 1. Verify Keycloak is running and realm is imported
curl http://localhost:8180/realms/ecommerce/.well-known/openid-configuration | python3 -m json.tool

# 2. Check required actions on admin account (should be empty)
docker exec ecommerce-keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password admin
docker exec ecommerce-keycloak /opt/keycloak/bin/kcadm.sh get users \
  -r ecommerce --query username=admin --fields username,enabled,requiredActions

# 3. Reset the admin password via Admin UI
# http://localhost:8180 → ecommerce realm → Users → admin → Credentials → Set password
# Set "Temporary" to OFF
```

### "Account is not fully set up" error on login
The user has a pending `UPDATE_PASSWORD` required action. Fix:
```bash
# Via Keycloak Admin UI: Users → admin → Details → Required user actions → remove UPDATE_PASSWORD
# Or via API:
curl -s -X PUT http://localhost:8180/admin/realms/ecommerce/users/<user-id> \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"requiredActions": []}'
```

### MCP service account returns 403 on admin endpoints
The `mcp-server` service account needs the `admin` realm role. This is pre-configured in `ecommerce-realm.json`.
If missing (e.g., after manual Keycloak changes):
```bash
# Assign admin role to service account via Admin UI
# Keycloak → ecommerce realm → Clients → mcp-server → Service account roles → Add admin
```

### "No products found" / empty list despite data in DB
The catalog service caches responses in Redis. If it started before seed data existed, an empty list is cached for 1 hour.
```bash
# Flush all Redis caches (dev only)
docker exec ecommerce-redis redis-cli FLUSHALL

# Verify data is actually in the DB
docker exec ecommerce-postgres psql -U app_user -d catalog_db \
  -c "SELECT COUNT(*) FROM products;"
```

### Alpine container healthcheck fails ("can't connect to remote host")
Alpine `wget` cannot resolve `localhost`. Use `127.0.0.1` and the container port (not the host-mapped port).
```yaml
# Correct healthcheck for Alpine-based containers
healthcheck:
  test: ["CMD", "wget", "-qO-", "http://127.0.0.1:80/health"]
```

### PowerShell curl doesn't work
PowerShell aliases `curl` to `Invoke-WebRequest`. Use Git Bash or:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/orders" `
  -Method GET -Headers @{ Authorization = "Bearer $TOKEN" }
```

### Service registered in Eureka but returns 404
The API Gateway routes all requests through `/api/{service-name}/...`. Verify the route prefix in `api-gateway/src/main/resources/application.yml`. Check Eureka dashboard at http://localhost:8761.

### Keycloak takes too long to start
Normal on first start (~60-90 seconds for DB init + realm import). Subsequent starts are faster (~30s).
```bash
# Follow startup progress
docker-compose logs -f keycloak | grep -E "started|error|WARN"
```

---

## Key Design Rules

1. **No cross-DB queries** — services only query their own database
2. **No synchronous calls for reads that can be cached** — use Redis object cache
3. **All order state changes via Outbox pattern** — never publish to RabbitMQ outside a DB transaction
4. **Idempotency-Key header** required on all order creation requests
5. **No static JWT keys** — Keycloak manages all key pairs; gateway uses JWKS endpoint
6. **Tests first** — write the test before the implementation for every feature
7. **Tests run against H2** for unit tests, **Testcontainers** for integration tests
8. **All auth through Keycloak** — no custom JWT issuance in application code

---

## Adding a New Service

1. Create the module directory under `backend-services/`
2. Add a `pom.xml` inheriting from the parent
3. Add the module to `backend-services/pom.xml`
4. Create a `Dockerfile` using the same multi-stage pattern as `discovery-service/Dockerfile`
5. Add the service to `docker-compose.yml` on `ecommerce-network`
6. Add its database to `infrastructure/postgres/init-databases.sql`
7. Register it in Eureka: add `spring-cloud-starter-netflix-eureka-client` dependency
8. Add a route in `api-gateway/src/main/resources/application.yml`

---

## Databases

Each service owns its database — no cross-service queries allowed.

```
postgresql://app_user:changeme@localhost:5432/{service}_db
```

| Service | Database |
|---------|----------|
| user-service | user_db |
| catalog-service | catalog_db |
| inventory-service | inventory_db |
| order-service | orders_db |
| order-processing-service | processing_db |
| notification-service | notification_db |
| analytics-service | analytics_db |
| Keycloak | keycloak_db (isolated, managed by Keycloak) |

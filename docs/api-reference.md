# API Reference

All requests go through the **API Gateway at `http://localhost:8080`**. The gateway validates Keycloak JWTs and injects `X-User-Id`, `X-User-Name`, and `X-User-Role` headers for downstream services.

## Authentication

```bash
# Get a bearer token (admin user)
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -d "grant_type=password&client_id=ecommerce-frontend&username=admin&password=admin123" \
  | jq -r '.access_token')

# All API calls
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/...
```

Endpoints marked **[admin]** require the `admin` Keycloak role.

---

## Catalog Service — `/api/catalog`

Direct service port: `8083`

### Products

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/catalog/products` | any | List products (paginated). Query params: `page`, `size`, `category` |
| GET | `/api/catalog/products/{id}` | any | Get product by ID |
| GET | `/api/catalog/products/search` | any | Search products. Query param: `q` (keyword) |
| POST | `/api/catalog/products` | [admin] | Create product |
| PUT | `/api/catalog/products/{id}` | [admin] | Update product |
| DELETE | `/api/catalog/products/{id}` | [admin] | Deactivate product |

**Example — search products**
```bash
curl "http://localhost:8080/api/catalog/products/search?q=dress" \
  -H "Authorization: Bearer $TOKEN"
```

**Example — create product [admin]**
```bash
curl -X POST http://localhost:8080/api/catalog/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Summer Dress",
    "description": "Lightweight cotton dress",
    "price": 49.99,
    "categoryId": 2,
    "sku": "DRESS-001"
  }'
```

### Categories

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/catalog/categories` | any | List categories. Query param: `rootOnly=true` for top-level only |

---

## Inventory Service — `/api/inventory`

Direct service port: `8084`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/inventory/{productId}/stock` | any | Get current stock for a product |
| POST | `/api/inventory/reserve` | internal | Reserve stock for an order |
| PUT | `/api/inventory/release/{orderId}` | internal | Release reserved stock (order cancelled) |
| PUT | `/api/inventory/confirm/{orderId}` | internal | Confirm stock deduction (order completed) |
| GET | `/api/inventory/admin/alerts` | [admin] | Products at or below reorder threshold |

**Example — check stock**
```bash
curl "http://localhost:8080/api/inventory/42/stock" \
  -H "Authorization: Bearer $TOKEN"
```

**Example — low stock alerts [admin]**
```bash
curl "http://localhost:8080/api/inventory/admin/alerts" \
  -H "Authorization: Bearer $TOKEN"
```

> `reserve`, `release`, and `confirm` are called internally by the Order and Order-Processing services. External callers should use order endpoints instead.

---

## Order Service — `/api/orders`

Direct service port: `8085`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/orders` | user | List orders for the authenticated user |
| GET | `/api/orders/{id}` | user | Get order by ID |
| POST | `/api/orders` | user | Create order. Requires `Idempotency-Key` header |
| PUT | `/api/orders/{id}/cancel` | user | Cancel an order |
| GET | `/api/orders/admin/summary` | [admin] | Revenue/order counts for a period |
| GET | `/api/orders/admin/search` | [admin] | Search orders by status, ID, or keyword |

**Example — create order**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "items": [
      {"productId": 42, "quantity": 2}
    ],
    "shippingAddress": {
      "street": "123 Main St",
      "city": "Springfield",
      "zip": "12345"
    }
  }'
```

**Example — order summary [admin]**
```bash
# period: today | 7d | 30d
curl "http://localhost:8080/api/orders/admin/summary?period=7d" \
  -H "Authorization: Bearer $TOKEN"
```

**Example — search orders [admin]**
```bash
curl "http://localhost:8080/api/orders/admin/search?q=PENDING" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Aggregator Service — `/api/aggregate`, `/api/analytics`

Direct service port: `8081`

### Dashboard & Sessions

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/aggregate/dashboard` | user | Aggregated dashboard data for the authenticated user |
| POST | `/api/aggregate/session/{userId}` | internal | Create/update user session |
| GET | `/api/aggregate/session/{userId}` | internal | Retrieve session data |
| DELETE | `/api/aggregate/session/{userId}` | internal | Invalidate session |

**Example — dashboard**
```bash
curl "http://localhost:8080/api/aggregate/dashboard" \
  -H "Authorization: Bearer $TOKEN"
```

### Analytics Events

These endpoints are called by the frontend to track user behaviour. They return `202 Accepted` immediately; events are delivered asynchronously via RabbitMQ.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/analytics/search-event` | user | Log a search action |
| POST | `/api/analytics/product-view-event` | user | Log a product view |

**Example — log search event**
```bash
curl -X POST http://localhost:8080/api/analytics/search-event \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "searchQuery": "summer dress",
    "resultCount": 12,
    "clickedProductId": 42
  }'
# → 202 Accepted
```

**Example — log product view**
```bash
curl -X POST http://localhost:8080/api/analytics/product-view-event \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 42,
    "sessionId": "sess-abc",
    "durationSeconds": 60,
    "source": "search"
  }'
# source: "search" | "category_browse" | "recommendation" | "direct"
# → 202 Accepted
```

---

## Order-Processing Service — `/api/order-processing`

Direct service port: `8086`

This service processes orders asynchronously via RabbitMQ. It exposes no public endpoints. It registers a heartbeat endpoint used by peers:

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/order-processing/heartbeat` | internal | Peer health ping |

---

## Analytics Service — `/api/analytics` (internal)

Direct service port: `8088`

Pure RabbitMQ consumer — no public REST endpoints. Exposes only:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/actuator/health` | Spring Boot health check |

---

## Notification Service — Python / FastAPI

Direct service port: `8087`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |

Triggered internally by RabbitMQ order events; no public-facing endpoints.

---

## MCP Server — Python / FastAPI

Direct service port: `8090`

Used by Claude via the Model Context Protocol. Also exposes direct HTTP endpoints for debugging:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |
| GET | `/tools/sales-summary` | Sales summary (`?period=today\|7d\|30d`) |
| GET | `/tools/low-inventory-alerts` | Products below reorder threshold |
| GET | `/tools/search-orders` | Order search (`?q=<query>`) |
| GET | `/tools/system-health` | Composite health across all services |

---

## Health Checks

Every Spring Boot service exposes `/actuator/health`. The API Gateway aggregates all into a composite check.

```bash
# Gateway composite health
curl http://localhost:8080/actuator/health | jq

# Individual service (direct, no auth needed for health)
curl http://localhost:8082/actuator/health   # user-service
curl http://localhost:8083/actuator/health   # catalog-service
curl http://localhost:8084/actuator/health   # inventory-service
curl http://localhost:8085/actuator/health   # order-service
curl http://localhost:8086/actuator/health   # order-processing-service
curl http://localhost:8088/actuator/health   # analytics-service
curl http://localhost:8087/health            # notification-service
curl http://localhost:8090/health            # mcp-server
```

---

## Service Ports Quick Reference

| Service | Host Port |
|---------|-----------|
| API Gateway | 8080 |
| Aggregator | 8081 |
| User | 8082 |
| Catalog | 8083 |
| Inventory | 8084 |
| Order | 8085 |
| Order-Processing | 8086 |
| Notification | 8087 |
| Analytics | 8088 |
| MCP Server | 8090 |
| Keycloak | 8180 |
| Eureka | 8761 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ UI | 15672 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Nginx | 80 |
| Frontend | 3000 |
| MailHog UI | 8025 |

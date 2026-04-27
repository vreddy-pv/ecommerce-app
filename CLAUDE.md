# E-Commerce Microservices — Developer Guide

## Architecture at a Glance

```
Browser/Mobile → [Nginx HTTP Cache :80] → [API Gateway :8080] → [Aggregator :8081]
                                                                         │
                     ┌───────────────────────────────────────────────────┤
                     ▼           ▼             ▼            ▼            ▼
               [Discovery  [User      [Catalog     [Inventory  [Order     [Order
                :8761]      :8082]     :8083]       :8084]      :8085]     Processing
                                        │                                   :8086]
                                   [Object Cache]                    ↕ heartbeats
                                   [Session Cache]
                                         │
                              [Notification :8087] (Python)
                              [MCP Server   :8090] (Python)
```

**Monitoring (two patterns)**:
- Hierarchical health checks: Gateway polls all services via `/actuator/health`
- Peer-to-peer heartbeats: Inventory ↔ Order Processing ↔ Order Service ping each other every 30s

**Caching (three layers)**:
- Layer 1 — Nginx HTTP cache (catalog products, categories)
- Layer 2 — Redis DB 0: Session cache (user sessions, owned by Aggregator)
- Layer 3 — Redis DB 1: Object cache (products, inventory, owned by Catalog/Inventory)

## Quick Start

```bash
# 1. Copy and configure environment
cp .env.example .env
# Edit .env with your JWT keys and passwords

# 2. Generate JWT RS256 key pair
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
# Base64-encode and add to .env:
#   JWT_PRIVATE_KEY=$(base64 -w0 private.pem)
#   JWT_PUBLIC_KEY=$(base64 -w0 public.pem)

# 3. Build and start all services
docker-compose up --build

# 4. Verify all services are up
docker-compose ps
```

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| Nginx (HTTP Cache) | 80 | http://localhost |
| API Gateway | 8080 | http://localhost:8080 |
| Aggregator Service | 8081 | http://localhost:8081 |
| User Service | 8082 | http://localhost:8082 |
| Catalog Service | 8083 | http://localhost:8083 |
| Inventory Service | 8084 | http://localhost:8084 |
| Order Service | 8085 | http://localhost:8085 |
| Order Processing | 8086 | http://localhost:8086 |
| Notification Service | 8087 | http://localhost:8087 |
| MCP Server | 8090 | http://localhost:8090 |
| Frontend | 3000 | http://localhost:3000 |
| Eureka Dashboard | 8761 | http://localhost:8761 |
| RabbitMQ Management | 15672 | http://localhost:15672 |
| MailHog (dev email) | 8025 | http://localhost:8025 |
| PostgreSQL | 5432 | localhost:5432 |
| Redis | 6379 | localhost:6379 |

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

## Docker Operations

```bash
# Start all services
docker-compose up -d

# Rebuild a single service after code change
docker-compose up -d --build catalog-service

# View logs
docker-compose logs -f catalog-service

# Stop all services
docker-compose down

# Stop and remove volumes (wipes all data)
docker-compose down -v

# Check service health
docker-compose ps
```

## Key Design Rules

1. **No cross-DB queries** — services only query their own database
2. **No synchronous calls for reads that can be cached** — use Redis object cache
3. **All order state changes via Outbox pattern** — never publish to RabbitMQ outside a DB transaction
4. **Idempotency-Key header** required on all order creation requests
5. **X-Internal-API-Key header** required on all service-to-service calls
6. **Tests first** — write the test before the implementation for every feature
7. **Tests run against H2** for unit tests, **Testcontainers** for integration tests

## Adding a New Service

1. Create the module directory under `backend-services/`
2. Add a `pom.xml` inheriting from the parent
3. Add the module to `backend-services/pom.xml`
4. Create a `Dockerfile` using the same multi-stage pattern as `discovery-service/Dockerfile`
5. Add the service to `docker-compose.yml` on `ecommerce-network`
6. Add its database to `infrastructure/postgres/init-databases.sql`
7. Register it in Eureka by adding `spring-cloud-starter-netflix-eureka-client` dependency

## Databases

Each service owns its database. Connect via:
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

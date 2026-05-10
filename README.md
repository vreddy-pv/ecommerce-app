# E-Commerce Microservices Platform

A full-stack e-commerce platform built with Spring Boot microservices, React, Keycloak SSO, and an AI assistant powered by Claude via the Model Context Protocol (MCP).

---

## Architecture

```
Browser/Mobile
      │
      ▼
[Keycloak :8180]  ──── SSO (Authorization Code + PKCE / Client Credentials)
      │
      ▼
[Nginx HTTP Cache :80]
      │
      ▼
[API Gateway :8080]  ──── JWT validation · header injection
      │
      ▼
[Aggregator :8081]  ──── orchestration · session cache · analytics publisher
      │
  ┌───┼───────────────────────────────────────┐
  ▼   ▼         ▼          ▼           ▼      ▼
User Catalog  Inventory  Order  Order-Processing  Notification
:8082 :8083    :8084     :8085      :8086          :8087
                │
          [Redis cache]
                                            │
                          RabbitMQ ─────────┘
                             │
                      Analytics :8088  ──── analytics_db

[MCP Server :8090]  ──── Claude AI tools (sales, inventory, orders, health)
```

**Three caching layers**: Nginx HTTP cache → Redis sessions (DB 0) → Redis object cache (DB 1)

**Two monitoring patterns**: Gateway hierarchical health checks + peer-to-peer heartbeats (Inventory ↔ Order ↔ Order-Processing)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript, Vite, Keycloak-JS |
| API Gateway | Spring Cloud Gateway |
| Microservices | Spring Boot 3.2, Java 17, Spring Cloud 2023 |
| Python Services | FastAPI, asyncio |
| Auth | Keycloak 24 (SSO, PKCE, Client Credentials) |
| Messaging | RabbitMQ 3.13 |
| Databases | PostgreSQL 16 (per-service), Redis 7 |
| Service Discovery | Netflix Eureka |
| HTTP Cache | Nginx 1.27 |
| AI Integration | Claude via MCP (Model Context Protocol) |
| Resilience | Resilience4j (circuit breaker, rate limiter) |

---

## Quick Start

**Prerequisites**: Docker, Docker Compose

```bash
# Clone and start everything
git clone <repo-url>
cd microservices
docker-compose up -d

# First start: wait ~60-90s for Keycloak to initialize
docker-compose logs -f keycloak | grep "started in"

# Verify all services are healthy
docker-compose ps
```

| URL | Purpose |
|-----|---------|
| http://localhost:3000 | Frontend (React app) |
| http://localhost:8080 | API Gateway |
| http://localhost:8180 | Keycloak Admin (`admin` / `admin`) |
| http://localhost:8761 | Eureka Dashboard |
| http://localhost:15672 | RabbitMQ Management (`admin` / `changeme`) |
| http://localhost:8025 | MailHog (dev email UI) |

**Default app login**: `admin` / `admin123` at http://localhost:3000

---

## Services

| Service | Port | Language | Description |
|---------|------|----------|-------------|
| frontend | 3000 | React/TS | Customer-facing UI |
| nginx | 80 | — | HTTP cache layer |
| api-gateway | 8080 | Java | Auth + routing |
| aggregator-service | 8081 | Java | Orchestration + session management |
| user-service | 8082 | Java | User account management |
| catalog-service | 8083 | Java | Products & categories |
| inventory-service | 8084 | Java | Stock levels, reservation |
| order-service | 8085 | Java | Order lifecycle |
| order-processing-service | 8086 | Java | Async order fulfilment |
| analytics-service | 8088 | Java | Event analytics consumer |
| notification-service | 8087 | Python | Email notifications |
| mcp-server | 8090 | Python | Claude AI integration |
| keycloak | 8180 | — | Identity provider |
| discovery-service | 8761 | Java | Eureka service registry |

---

## Project Structure

```
microservices/
├── backend-services/          # Java/Spring Boot services
│   ├── common-lib/            # Shared DTOs and utilities
│   ├── discovery-service/     # Eureka server
│   ├── api-gateway/           # Spring Cloud Gateway
│   ├── aggregator-service/    # Orchestrator
│   ├── user-service/
│   ├── catalog-service/
│   ├── inventory-service/
│   ├── order-service/
│   ├── order-processing-service/
│   └── analytics-service/
├── python-services/
│   ├── notification-service/  # FastAPI + RabbitMQ consumer
│   └── mcp-server/            # Claude MCP tools
├── frontend/                  # React + Vite
├── infrastructure/
│   ├── keycloak/              # Realm export (auto-imported)
│   ├── nginx/                 # nginx.conf + cache dir
│   ├── postgres/              # DB init script
│   ├── rabbitmq/              # Broker config + definitions
│   └── redis/                 # redis.conf
├── docker-compose.yml
├── .mcp.json                  # Claude Code MCP config (auto-loaded)
└── CLAUDE.md                  # Full developer guide
```

---

## Authentication

All user auth flows through Keycloak — no custom JWT issuance in application code.

```
# Get a token for API testing (bash)
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/ecommerce/protocol/openid-connect/token \
  -d "grant_type=password&client_id=ecommerce-frontend&username=admin&password=admin123" \
  | jq -r '.access_token')

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/orders
```

Roles: `user` (default for all registrations) · `admin` (access to `/api/*/admin/**`)

---

## AI Assistant (MCP)

Claude can query sales data, inventory alerts, orders, and system health directly via MCP tools. The MCP server authenticates with Keycloak using Client Credentials — no user login required.

```bash
# Use Claude Code (MCP config is auto-loaded from .mcp.json)
cd microservices
claude
```

See [CLAUDE.md](CLAUDE.md) for the full MCP configuration and Claude Desktop setup.

---

## Running Tests

```bash
# Java services (unit tests — H2 in-memory)
cd backend-services && ./mvnw test

# Java services (integration tests — Testcontainers)
cd backend-services && ./mvnw verify

# Python services
cd python-services/notification-service && pytest tests/ -v --cov
cd python-services/mcp-server && pytest tests/ -v --cov

# Frontend
cd frontend && npm test -- --coverage
```

---

## Developer Guide

See **[CLAUDE.md](CLAUDE.md)** for:
- Full architecture details and design rules
- Keycloak SSO configuration and token troubleshooting
- Analytics pipeline internals
- Common issues and fixes
- Adding a new service
- Docker operations reference

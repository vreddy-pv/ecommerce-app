# Reset Ecommerce Stack

Stop all services, remove data volumes, and restart fresh. Use this to recover from a broken state or re-import the Keycloak realm.

## ⚠️ Warning
This destroys ALL local data: databases, Redis cache, RabbitMQ queues.
The Keycloak realm will be re-imported from `infrastructure/keycloak/ecommerce-realm.json`.

## Steps

1. Confirm with the user that they want to wipe all data — ask explicitly before proceeding
2. Run `docker-compose down -v` from `C:/Veera/AI/agents/microservices/`
3. Run `docker-compose up -d`
4. Wait for Keycloak to be ready (watch logs: `docker-compose logs -f keycloak | grep "started in"`)
5. Run `docker-compose ps` to confirm all containers are healthy
6. Report the final status and access URLs

## Selective reset (softer options)
```bash
# Just restart one service (no data loss)
docker-compose restart keycloak

# Rebuild one service after code change (no data loss)
docker-compose up -d --build catalog-service

# Flush Redis cache only (keeps DB data)
docker exec ecommerce-redis redis-cli FLUSHALL

# Rebuild Java service
cd backend-services && ./mvnw clean package -pl catalog-service -am -DskipTests
docker-compose up -d --build catalog-service
```

## After reset — default credentials
| Resource | Username | Password |
|----------|----------|----------|
| App (frontend) | admin | admin123 |
| Keycloak Admin UI | admin | admin |
| RabbitMQ | admin | changeme |
| PostgreSQL | app_user | changeme |

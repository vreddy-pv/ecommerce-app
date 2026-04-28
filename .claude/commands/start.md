# Start All Services

Start the full ecommerce stack with Docker Compose and wait for all services to be healthy.

## Steps

1. Run `docker-compose up -d` from `C:/Veera/AI/agents/microservices/`
2. Watch Keycloak startup — it can take 60-90 seconds on first start
3. Run `docker-compose ps` to show the health status of all 18 containers
4. Report any containers that are NOT healthy with their logs (last 20 lines)
5. Once all services are healthy, print the key access URLs:
   - Frontend:          http://localhost:3000
   - API Gateway:       http://localhost:8080
   - Keycloak Admin UI: http://localhost:8180  (admin / admin)
   - Eureka Dashboard:  http://localhost:8761
   - RabbitMQ:          http://localhost:15672 (admin / changeme)
   - MailHog:           http://localhost:8025

## Notes
- Default app login: admin / admin123 (ecommerce realm)
- If a container is stuck unhealthy, run `docker-compose restart <service-name>`
- If Keycloak fails to import realm, run `docker-compose down -v && docker-compose up -d`

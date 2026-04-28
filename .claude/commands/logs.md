# View Service Logs

Stream or display logs for one or more ecommerce services.

## Usage
```
/logs <service-name> [lines]
/logs api-gateway 50
/logs keycloak
/logs order-service order-processing-service
```

## Steps

1. Parse $ARGUMENTS for service name(s) and optional line count (default: 50)
2. If no service name given, ask which service the user wants to see
3. Run `docker-compose logs --tail=<lines> <service-name>` from `C:/Veera/AI/agents/microservices/`
4. Display the output and highlight any ERROR or WARN lines
5. If the user wants to follow logs in real time, run `docker-compose logs -f <service-name>` — note that this blocks, so mention they can Ctrl+C to stop

## Valid service names
| Short name | Docker container |
|-----------|-----------------|
| `gateway` or `api-gateway` | ecommerce-api-gateway |
| `keycloak` | ecommerce-keycloak |
| `frontend` | ecommerce-frontend |
| `catalog` | ecommerce-catalog-service |
| `inventory` | ecommerce-inventory-service |
| `order` | ecommerce-order-service |
| `order-processing` | ecommerce-order-processing-service |
| `user` | ecommerce-user-service |
| `aggregator` | ecommerce-aggregator-service |
| `notification` | ecommerce-notification-service |
| `mcp` or `mcp-server` | ecommerce-mcp-server |
| `rabbitmq` | ecommerce-rabbitmq |
| `redis` | ecommerce-redis |
| `postgres` | ecommerce-postgres |
| `nginx` | ecommerce-nginx |
| `eureka` | ecommerce-discovery |

## Notes
- For auth issues: check `keycloak` and `api-gateway` logs together
- For order failures: check `order-service` and `order-processing-service` together
- For missing data: check `catalog-service` and `redis` logs

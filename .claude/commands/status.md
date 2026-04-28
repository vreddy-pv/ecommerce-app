# Ecommerce Stack Status

Show a complete health snapshot of the ecommerce stack — containers, services, and live data.

## Steps

1. Run `docker-compose ps` and display the table of all containers with their health state
2. For any container that is **unhealthy** or **exited**, show the last 30 log lines
3. Call the MCP tool `get_system_health` to get the API Gateway's composite health view
4. Call the MCP tool `get_sales_summary` with period=`7d` to confirm the backend is serving data
5. Call the MCP tool `get_low_inventory_alerts` to show current stock warnings
6. Summarize in a table:

| Layer | Component | Status |
|-------|-----------|--------|
| Infrastructure | PostgreSQL, Redis, RabbitMQ, Keycloak | |
| Backend | Gateway, Catalog, Inventory, Order, User | |
| Frontend | Nginx, React app | |
| AI Integration | MCP Server | |

## Notes
- Green = healthy/UP, Red = unhealthy/DOWN, Yellow = starting
- If `get_system_health` fails, the API Gateway or Keycloak is not ready yet
- Wait 90 seconds after `docker-compose up` before checking status

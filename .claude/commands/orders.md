# Order Dashboard

Show a live order and sales dashboard using MCP tools.

## Usage
```
/orders              → full dashboard (7-day summary + alerts + recent orders)
/orders today        → today's summary only
/orders 30d          → 30-day summary
/orders search <q>   → search orders (e.g. /orders search PENDING)
```

## Steps based on $ARGUMENTS

### No argument or period (`today`, `7d`, `30d`)
1. Call MCP tool `get_sales_summary` with the requested period (default: `7d`)
2. Call MCP tool `get_low_inventory_alerts` to show stock warnings
3. Call MCP tool `get_system_health` to confirm all services are UP
4. Present a formatted dashboard:

```
📊 Sales Summary — Last 7 Days
─────────────────────────────
Total Orders:   9
Total Revenue:  $584.91

Order Breakdown:
  ✅ Confirmed:   9
  ⏳ Pending:     0
  🔄 Processing:  0
  🚚 Shipped:     0
  📦 Delivered:   0
  ❌ Cancelled:   0

⚠️  Low Inventory Alerts: <count>
🟢 System Health: UP
```

### `search <query>`
1. Call MCP tool `search_orders` with the query string
2. Display results as a table: Order ID, Status, User, Amount, Date
3. If no results, suggest alternative search terms

## Notes
- All data is fetched live via the MCP server → API Gateway → Order/Inventory services
- The MCP server authenticates automatically via Keycloak Client Credentials (no login needed)
- Requires Docker services to be running (`/start` or `docker-compose up -d`)

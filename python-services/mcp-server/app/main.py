import logging
from fastapi import FastAPI, Query, HTTPException
from . import tools

logging.basicConfig(level=logging.INFO)

app = FastAPI(title="MCP Server — E-Commerce AI Tools")


@app.get("/health")
def health():
    return {"status": "UP", "service": "mcp-server"}


@app.get("/tools/sales-summary")
async def sales_summary(period: str = Query("7d", description="Period: today, 7d, 30d")):
    try:
        return await tools.get_sales_summary(period)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc))


@app.get("/tools/low-inventory-alerts")
async def low_inventory_alerts():
    try:
        return await tools.get_low_inventory_alerts()
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc))


@app.get("/tools/search-orders")
async def search_orders(q: str = Query(..., description="Natural language order query")):
    try:
        return await tools.search_orders(q)
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc))


@app.get("/tools/system-health")
async def system_health():
    try:
        return await tools.get_system_health()
    except Exception as exc:
        raise HTTPException(status_code=502, detail=str(exc))

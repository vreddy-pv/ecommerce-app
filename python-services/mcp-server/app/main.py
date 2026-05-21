import logging
from fastapi import FastAPI, Query, HTTPException
from fastapi.responses import JSONResponse
from . import tools
from . import chat
from .logging_config import setup_logging, agent_logger
from .metrics import get_metrics

# Setup structured logging
setup_logging(level="INFO")
logger = agent_logger()

app = FastAPI(title="MCP Server + Autonomous Agent — E-Commerce")

# Mount the agent chat app at /api/ai
app.mount("/api/ai", chat.app)


@app.get("/health")
def health():
    metrics = get_metrics()
    return {
        "status": "UP",
        "service": "mcp-server",
        "metrics": {
            "chat_requests": metrics.chat_requests,
            "active_sessions": len(metrics.active_sessions),
            "error_rate": (
                (metrics.chat_errors / metrics.chat_requests * 100)
                if metrics.chat_requests > 0
                else 0
            ),
        },
    }


@app.get("/metrics")
def metrics_endpoint():
    """Prometheus-compatible metrics endpoint."""
    metrics = get_metrics()
    return JSONResponse(content=metrics.to_dict())


@app.post("/errors/log")
async def log_frontend_error(error_data: dict):
    """
    Endpoint for frontend to report errors.
    Called by ErrorBoundary component.
    """
    logger.error(
        "Frontend error",
        error_message=error_data.get("message"),
        error_stack=error_data.get("stack"),
        component_stack=error_data.get("componentStack"),
        user_agent=error_data.get("userAgent"),
    )
    return {"status": "logged"}


# ============================================================================
# Legacy MCP tool endpoints (read-only, no agent involved)
# ============================================================================

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

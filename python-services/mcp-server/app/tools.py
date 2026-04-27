import httpx
from typing import Any
from .config import settings


async def get_sales_summary(period: str) -> dict[str, Any]:
    """Fetch aggregated sales totals for a given period (e.g. 'today', '7d', '30d')."""
    async with httpx.AsyncClient() as client:
        resp = await client.get(
            f"{settings.order_url}/api/orders/summary",
            params={"period": period},
            timeout=10,
        )
        resp.raise_for_status()
        return resp.json()


async def get_low_inventory_alerts() -> list[dict[str, Any]]:
    """Return products whose available stock is at or below the reorder threshold."""
    async with httpx.AsyncClient() as client:
        resp = await client.get(
            f"{settings.inventory_url}/api/inventory/alerts",
            timeout=10,
        )
        resp.raise_for_status()
        return resp.json()


async def search_orders(query: str) -> list[dict[str, Any]]:
    """Natural-language order search: delegates to order-service full-text endpoint."""
    async with httpx.AsyncClient() as client:
        resp = await client.get(
            f"{settings.order_url}/api/orders/search",
            params={"q": query},
            timeout=10,
        )
        resp.raise_for_status()
        return resp.json()


async def get_system_health() -> dict[str, Any]:
    """Fetch composite system health from the API gateway (hierarchical monitoring)."""
    async with httpx.AsyncClient() as client:
        resp = await client.get(
            f"{settings.gateway_url}/actuator/health",
            timeout=10,
        )
        resp.raise_for_status()
        return resp.json()

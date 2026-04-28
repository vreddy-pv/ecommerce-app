"""
MCP tool implementations.

All HTTP calls are routed through the API Gateway so that:
- JWT authentication is enforced end-to-end
- The MCP server does NOT bypass gateway security

Authentication uses OAuth2 Client Credentials (mcp-server Keycloak client).
The MCPAuthManager fetches and caches the service-account token automatically.
"""
import httpx
from typing import Any

from .auth import MCPAuthManager
from .config import settings


# Module-level auth manager — persists for the lifetime of the MCP stdio process
_auth = MCPAuthManager(
    keycloak_url=settings.keycloak_url,
    realm=settings.keycloak_realm,
    client_id=settings.mcp_client_id,
    client_secret=settings.mcp_client_secret,
)


def _unwrap(resp_json: dict) -> Any:
    """Unwrap Spring ApiResponse: { data: ..., success: bool, ... }"""
    return resp_json.get("data", resp_json)


async def _get(url: str, **kwargs: Any) -> Any:
    """
    Authenticated GET with automatic 401-retry.
    On a 401 the token is invalidated and one re-login is attempted.
    """
    for attempt in range(2):
        token = await _auth.get_token()
        headers = {"Authorization": f"Bearer {token}"}
        async with httpx.AsyncClient() as client:
            resp = await client.get(url, headers=headers, timeout=10, **kwargs)
        if resp.status_code == 401 and attempt == 0:
            _auth.invalidate()
            continue
        resp.raise_for_status()
        return _unwrap(resp.json())
    # Should not reach here
    raise RuntimeError("Authentication failed after retry")


async def get_sales_summary(period: str) -> dict[str, Any]:
    """Fetch aggregated sales totals for a given period ('today', '7d', '30d')."""
    return await _get(
        f"{settings.gateway_url}/api/orders/admin/summary",
        params={"period": period},
    )


async def get_low_inventory_alerts() -> list[dict[str, Any]]:
    """Return products whose available stock is at or below the reorder threshold."""
    return await _get(f"{settings.gateway_url}/api/inventory/admin/alerts")


async def search_orders(query: str) -> list[dict[str, Any]]:
    """
    Search orders by status name (e.g. 'PENDING'), order ID, or return recent 20.
    """
    return await _get(
        f"{settings.gateway_url}/api/orders/admin/search",
        params={"q": query},
    )


async def get_system_health() -> dict[str, Any]:
    """Fetch composite system health from the API gateway (hierarchical monitoring)."""
    # /actuator/health is a public path — auth header is sent for consistency but not required
    token = await _auth.get_token()
    headers = {"Authorization": f"Bearer {token}"}
    async with httpx.AsyncClient() as client:
        resp = await client.get(
            f"{settings.gateway_url}/actuator/health",
            headers=headers,
            timeout=10,
        )
        resp.raise_for_status()
        return resp.json()

"""
Agent tool implementations wrapping the microservices.
Each tool makes authenticated calls to the API gateway.
Tools are decorated with @tool for LangGraph integration.
"""
import httpx
import json
from typing import Any
from .auth import MCPAuthManager
from .config import settings


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
    """Authenticated GET with automatic 401-retry."""
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
    raise RuntimeError("Authentication failed after retry")


async def _post(url: str, data: dict, **kwargs: Any) -> Any:
    """Authenticated POST."""
    for attempt in range(2):
        token = await _auth.get_token()
        headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
        async with httpx.AsyncClient() as client:
            resp = await client.post(url, json=data, headers=headers, timeout=10, **kwargs)
        if resp.status_code == 401 and attempt == 0:
            _auth.invalidate()
            continue
        resp.raise_for_status()
        return _unwrap(resp.json())
    raise RuntimeError("Authentication failed after retry")


async def get_order_details(order_id: str) -> dict[str, Any]:
    """
    Retrieve full details for a specific order including items, status, customer info.
    Tool for: detailed order analysis, fraud detection context.
    """
    return await _get(f"{settings.gateway_url}/api/orders/admin/{order_id}")


async def get_recent_orders(user_id: str | None = None, limit: int = 10) -> list[dict[str, Any]]:
    """
    Retrieve recent orders (optionally filtered by user_id).
    Tool for: checking customer order history, fraud pattern detection.
    """
    params = {"limit": limit}
    if user_id:
        params["user_id"] = user_id
    return await _get(f"{settings.gateway_url}/api/orders/admin/recent", params=params)


async def check_fraud_score(order_id: str) -> dict[str, Any]:
    """
    Get fraud risk score (0-100) and risk factors for an order.
    HIGH RISK: score > 70. LOW RISK: < 30. MEDIUM: 30-70.
    Always check before approving high-value or suspicious orders.
    """
    result = await _get(f"{settings.gateway_url}/api/orders/admin/{order_id}/fraud-score")
    # Ensure risk level is set based on score
    if isinstance(result, dict) and "score" in result:
        score = result["score"]
        if score > 70:
            result["risk_level"] = "HIGH"
        elif score < 30:
            result["risk_level"] = "LOW"
        else:
            result["risk_level"] = "MEDIUM"
    return result


async def check_inventory_levels(product_id: str | None = None) -> dict[str, Any] | list[dict[str, Any]]:
    """
    Check inventory levels for a product, or return low-stock alerts.
    Includes stock count, reorder point, warehouse locations.
    """
    if product_id:
        return await _get(f"{settings.gateway_url}/api/inventory/admin/products/{product_id}")
    else:
        return await _get(f"{settings.gateway_url}/api/inventory/admin/alerts")


async def cancel_order(order_id: str, reason: str = "FRAUD_SUSPECTED") -> dict[str, Any]:
    """
    Cancel an order. Only call after fraud detection or customer request.
    Reason: FRAUD_SUSPECTED, OUT_OF_STOCK, CUSTOMER_REQUEST, etc.
    NOTE: This action requires user confirmation on the frontend.
    """
    data = {"reason": reason, "action_by": "ai-agent"}
    return await _post(f"{settings.gateway_url}/api/orders/admin/{order_id}/cancel", data)


async def update_inventory(product_id: str, quantity: int, reason: str = "CORRECTION") -> dict[str, Any]:
    """
    Update stock level for a product (e.g., after discovering shrinkage or recount).
    quantity: new absolute stock count
    reason: CORRECTION, RECOUNT, DAMAGE_REPORTED, etc.
    NOTE: This action requires user confirmation on the frontend.
    """
    data = {"quantity": quantity, "reason": reason, "action_by": "ai-agent"}
    return await _post(f"{settings.gateway_url}/api/inventory/admin/products/{product_id}/update-stock", data)


async def flag_for_review(order_id: str, reason: str, risk_score: float | None = None) -> dict[str, Any]:
    """
    Flag an order for manual review by a human agent.
    Reason: HIGH_FRAUD_SCORE, UNUSUAL_PATTERN, CUSTOMER_DISPUTE, etc.
    This is a safe action (no approval required) — just marks order for review.
    """
    data = {
        "reason": reason,
        "risk_score": risk_score,
        "flagged_by": "ai-agent",
    }
    return await _post(f"{settings.gateway_url}/api/orders/admin/{order_id}/flag-review", data)


# Tool list for export to agents.py
TOOL_DEFINITIONS = [
    {
        "name": "get_order_details",
        "description": "Retrieve full details for a specific order",
        "func": get_order_details,
    },
    {
        "name": "get_recent_orders",
        "description": "Retrieve recent orders optionally filtered by user_id",
        "func": get_recent_orders,
    },
    {
        "name": "check_fraud_score",
        "description": "Get fraud risk score (0-100) for an order",
        "func": check_fraud_score,
    },
    {
        "name": "check_inventory_levels",
        "description": "Check inventory levels for a product or get low-stock alerts",
        "func": check_inventory_levels,
    },
    {
        "name": "cancel_order",
        "description": "Cancel an order (requires user confirmation)",
        "func": cancel_order,
    },
    {
        "name": "update_inventory",
        "description": "Update stock level for a product (requires user confirmation)",
        "func": update_inventory,
    },
    {
        "name": "flag_for_review",
        "description": "Flag an order for manual review (no approval required)",
        "func": flag_for_review,
    },
]

import pytest
import respx
import httpx
from app.tools import get_sales_summary, get_low_inventory_alerts, search_orders, get_system_health
from app.config import settings


@respx.mock
@pytest.mark.asyncio
async def test_get_sales_summary_returns_data():
    respx.get(f"{settings.order_url}/api/orders/summary").mock(
        return_value=httpx.Response(200, json={"period": "7d", "totalRevenue": 9999.99, "orderCount": 42})
    )
    result = await get_sales_summary("7d")
    assert result["orderCount"] == 42
    assert result["totalRevenue"] == 9999.99


@respx.mock
@pytest.mark.asyncio
async def test_get_sales_summary_propagates_http_error():
    respx.get(f"{settings.order_url}/api/orders/summary").mock(
        return_value=httpx.Response(500, json={"error": "internal"})
    )
    with pytest.raises(httpx.HTTPStatusError):
        await get_sales_summary("7d")


@respx.mock
@pytest.mark.asyncio
async def test_get_low_inventory_alerts_returns_list():
    respx.get(f"{settings.inventory_url}/api/inventory/alerts").mock(
        return_value=httpx.Response(200, json=[
            {"productId": 10, "available": 2, "threshold": 10},
            {"productId": 11, "available": 0, "threshold": 5},
        ])
    )
    result = await get_low_inventory_alerts()
    assert len(result) == 2
    assert result[0]["productId"] == 10


@respx.mock
@pytest.mark.asyncio
async def test_search_orders_returns_matches():
    respx.get(f"{settings.order_url}/api/orders/search").mock(
        return_value=httpx.Response(200, json=[{"orderId": 1, "status": "SHIPPED"}])
    )
    result = await search_orders("recent laptop orders")
    assert result[0]["orderId"] == 1


@respx.mock
@pytest.mark.asyncio
async def test_get_system_health_returns_composite_status():
    respx.get(f"{settings.gateway_url}/actuator/health").mock(
        return_value=httpx.Response(200, json={"status": "UP", "components": {"db": {"status": "UP"}}})
    )
    result = await get_system_health()
    assert result["status"] == "UP"


@respx.mock
@pytest.mark.asyncio
async def test_get_system_health_propagates_degraded_status():
    respx.get(f"{settings.gateway_url}/actuator/health").mock(
        return_value=httpx.Response(200, json={"status": "DOWN"})
    )
    result = await get_system_health()
    assert result["status"] == "DOWN"

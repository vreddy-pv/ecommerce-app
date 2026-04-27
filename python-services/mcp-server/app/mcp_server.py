"""
Stdio-based MCP entry point for Claude Desktop.
Exposes the same 4 tools as the FastAPI HTTP server but over MCP protocol.

Run:  python -m app.mcp_server   (or via claude_desktop_config.json)
HTTP server (docker):  uvicorn app.main:app
"""
from mcp.server.fastmcp import FastMCP
from . import tools

mcp = FastMCP("ecommerce")


@mcp.tool()
async def get_sales_summary(period: str = "7d") -> dict:
    """
    Return aggregated sales totals for a given period.
    period: 'today', '7d', or '30d'
    """
    return await tools.get_sales_summary(period)


@mcp.tool()
async def get_low_inventory_alerts() -> list:
    """Return products whose available stock is at or below the reorder threshold."""
    return await tools.get_low_inventory_alerts()


@mcp.tool()
async def search_orders(q: str) -> list:
    """Search orders using a natural-language query (e.g. 'last 5 orders for user 42')."""
    return await tools.search_orders(q)


@mcp.tool()
async def get_system_health() -> dict:
    """Fetch composite health from the API gateway — shows UP/DOWN for every service."""
    return await tools.get_system_health()


if __name__ == "__main__":
    mcp.run()

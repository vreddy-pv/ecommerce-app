from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # API Gateway — all tool calls route through here
    gateway_url: str = "http://api-gateway:8080"

    # Keycloak OAuth2 Client Credentials for the MCP service account
    keycloak_url: str = "http://keycloak:8080"
    keycloak_realm: str = "ecommerce"
    mcp_client_id: str = "mcp-server"
    mcp_client_secret: str = ""

    # Claude API (for autonomous agent)
    anthropic_api_key: str = ""

    # Redis (optional; if not set, uses in-memory storage)
    redis_url: str = ""  # e.g., "redis://redis:6379/0"

    model_config = {"env_file": ".env"}


settings = Settings()

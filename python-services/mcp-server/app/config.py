from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    anthropic_api_key: str = "sk-ant-placeholder"
    gateway_url: str = "http://api-gateway:8080"
    catalog_url: str = "http://catalog-service:8083"
    inventory_url: str = "http://inventory-service:8084"
    order_url: str = "http://order-service:8085"

    model_config = {"env_file": ".env"}


settings = Settings()

from pydantic import model_validator
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "admin"
    rabbitmq_pass: str = "changeme"
    rabbitmq_url: str = ""  # built from parts below if not set directly

    database_url: str = "postgresql+asyncpg://app_user:changeme@localhost:5432/notif_db"
    orders_exchange: str = "orders.exchange"
    order_confirmed_queue: str = "notification.order.confirmed"
    order_shipped_queue: str = "notification.order.shipped"
    order_failed_queue: str = "notification.order.failed"
    smtp_host: str = "localhost"
    smtp_port: int = 1025
    smtp_from: str = "noreply@ecommerce.local"

    model_config = {"env_file": ".env"}

    @model_validator(mode="after")
    def build_rabbitmq_url(self) -> "Settings":
        if not self.rabbitmq_url:
            self.rabbitmq_url = (
                f"amqp://{self.rabbitmq_user}:{self.rabbitmq_pass}"
                f"@{self.rabbitmq_host}:{self.rabbitmq_port}/"
            )
        return self


settings = Settings()

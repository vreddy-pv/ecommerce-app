import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from .config import settings
from .database import AsyncSessionLocal, engine
from .models import Base
from .email_service import email_service
from .consumer import NotificationConsumer, start_consuming

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    consumer = NotificationConsumer(email_service)
    try:
        await start_consuming(consumer, AsyncSessionLocal)
    except Exception as exc:
        log.warning("RabbitMQ not available at startup: %s", exc)
    yield


app = FastAPI(title="Notification Service", lifespan=lifespan)


@app.get("/health")
def health():
    return {"status": "UP", "service": "notification-service"}

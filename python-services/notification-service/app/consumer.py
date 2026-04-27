import json
import logging
import aio_pika
from sqlalchemy.ext.asyncio import AsyncSession

from .config import settings
from .email_service import EmailService
from .models import NotificationLog

log = logging.getLogger(__name__)


class NotificationConsumer:
    def __init__(self, email_svc: EmailService):
        self.email_svc = email_svc

    async def handle_order_confirmed(self, message: aio_pika.IncomingMessage, session: AsyncSession) -> None:
        async with message.process():
            event = json.loads(message.body)
            user_id = event.get("userId")
            order_id = event.get("orderId")
            log.info("order.confirmed received — orderId=%s userId=%s", order_id, user_id)

            success = self.email_svc.send_order_confirmation(user_id, order_id)
            entry = NotificationLog(
                user_id=user_id,
                order_id=order_id,
                type="EMAIL",
                status="SENT" if success else "FAILED",
            )
            session.add(entry)
            await session.commit()

    async def handle_order_shipped(self, message: aio_pika.IncomingMessage, session: AsyncSession) -> None:
        async with message.process():
            event = json.loads(message.body)
            user_id = event.get("userId")
            order_id = event.get("orderId")
            tracking = event.get("trackingNumber", "N/A")
            log.info("order.shipped received — orderId=%s", order_id)

            success = self.email_svc.send_tracking_info(user_id, order_id, tracking)
            entry = NotificationLog(
                user_id=user_id,
                order_id=order_id,
                type="EMAIL",
                status="SENT" if success else "FAILED",
            )
            session.add(entry)
            await session.commit()

    async def handle_order_failed(self, message: aio_pika.IncomingMessage, session: AsyncSession) -> None:
        async with message.process():
            event = json.loads(message.body)
            user_id = event.get("userId")
            order_id = event.get("orderId")
            log.info("order.failed received — orderId=%s", order_id)

            success = self.email_svc.send_order_failed(user_id, order_id)
            entry = NotificationLog(
                user_id=user_id,
                order_id=order_id,
                type="EMAIL",
                status="SENT" if success else "FAILED",
            )
            session.add(entry)
            await session.commit()


async def start_consuming(consumer: NotificationConsumer, session_factory) -> None:
    connection = await aio_pika.connect_robust(settings.rabbitmq_url)
    channel = await connection.channel()
    exchange = await channel.declare_exchange(
        settings.orders_exchange, aio_pika.ExchangeType.TOPIC, durable=True
    )

    for queue_name, routing_key, handler in [
        (settings.order_confirmed_queue, "order.confirmed", consumer.handle_order_confirmed),
        (settings.order_shipped_queue, "order.shipped", consumer.handle_order_shipped),
        (settings.order_failed_queue, "order.failed", consumer.handle_order_failed),
    ]:
        queue = await channel.declare_queue(queue_name, durable=True)
        await queue.bind(exchange, routing_key)
        async with session_factory() as session:
            await queue.consume(lambda msg, s=session, h=handler: h(msg, s))

    log.info("Notification consumer started — listening on %s queues", 3)

import json
import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from app.consumer import NotificationConsumer
from app.email_service import EmailService


def _make_message(payload: dict) -> MagicMock:
    msg = MagicMock()
    msg.body = json.dumps(payload).encode()
    msg.process = MagicMock(return_value=AsyncMock().__aenter__.return_value)
    msg.process().__aenter__ = AsyncMock(return_value=None)
    msg.process().__aexit__ = AsyncMock(return_value=False)
    return msg


@pytest.fixture
def email_mock():
    svc = MagicMock(spec=EmailService)
    svc.send_order_confirmation.return_value = True
    svc.send_tracking_info.return_value = True
    svc.send_order_failed.return_value = True
    return svc


@pytest.fixture
def consumer(email_mock):
    return NotificationConsumer(email_mock)


@pytest.fixture
def session():
    s = AsyncMock()
    s.add = MagicMock()
    s.commit = AsyncMock()
    return s


@pytest.mark.asyncio
async def test_handle_order_confirmed_sends_email(consumer, email_mock, session):
    msg = _make_message({"userId": 1, "orderId": 42})
    await consumer.handle_order_confirmed(msg, session)
    email_mock.send_order_confirmation.assert_called_once_with(1, 42)


@pytest.mark.asyncio
async def test_handle_order_confirmed_logs_sent_status(consumer, session):
    msg = _make_message({"userId": 1, "orderId": 42})
    await consumer.handle_order_confirmed(msg, session)
    session.add.assert_called_once()
    log_entry = session.add.call_args[0][0]
    assert log_entry.status == "SENT"
    assert log_entry.order_id == 42
    assert log_entry.type == "EMAIL"


@pytest.mark.asyncio
async def test_handle_order_confirmed_logs_failed_when_smtp_down(consumer, email_mock, session):
    email_mock.send_order_confirmation.return_value = False
    msg = _make_message({"userId": 1, "orderId": 42})
    await consumer.handle_order_confirmed(msg, session)
    log_entry = session.add.call_args[0][0]
    assert log_entry.status == "FAILED"


@pytest.mark.asyncio
async def test_handle_order_shipped_sends_tracking_email(consumer, email_mock, session):
    msg = _make_message({"userId": 1, "orderId": 42, "trackingNumber": "TRK-001"})
    await consumer.handle_order_shipped(msg, session)
    email_mock.send_tracking_info.assert_called_once_with(1, 42, "TRK-001")


@pytest.mark.asyncio
async def test_handle_order_failed_sends_failure_email(consumer, email_mock, session):
    msg = _make_message({"userId": 1, "orderId": 42})
    await consumer.handle_order_failed(msg, session)
    email_mock.send_order_failed.assert_called_once_with(1, 42)

import pytest
from unittest.mock import patch, MagicMock
from app.email_service import EmailService


@pytest.fixture
def svc():
    return EmailService()


def test_send_order_confirmation_returns_true_when_smtp_ok(svc):
    with patch("app.email_service.smtplib.SMTP") as mock_smtp:
        mock_smtp.return_value.__enter__ = MagicMock(return_value=MagicMock())
        mock_smtp.return_value.__exit__ = MagicMock(return_value=False)
        result = svc.send_order_confirmation(user_id=1, order_id=42)
    assert result is True


def test_send_order_confirmation_returns_false_when_smtp_fails(svc):
    with patch("app.email_service.smtplib.SMTP", side_effect=ConnectionRefusedError("down")):
        result = svc.send_order_confirmation(user_id=1, order_id=42)
    assert result is False


def test_send_tracking_info_returns_true_when_smtp_ok(svc):
    with patch("app.email_service.smtplib.SMTP") as mock_smtp:
        mock_smtp.return_value.__enter__ = MagicMock(return_value=MagicMock())
        mock_smtp.return_value.__exit__ = MagicMock(return_value=False)
        result = svc.send_tracking_info(user_id=1, order_id=42, tracking_number="TRK-001")
    assert result is True


def test_send_order_failed_returns_true_when_smtp_ok(svc):
    with patch("app.email_service.smtplib.SMTP") as mock_smtp:
        mock_smtp.return_value.__enter__ = MagicMock(return_value=MagicMock())
        mock_smtp.return_value.__exit__ = MagicMock(return_value=False)
        result = svc.send_order_failed(user_id=1, order_id=42)
    assert result is True


def test_send_retries_not_attempted_on_first_failure(svc):
    """EmailService makes exactly one attempt — no silent retry loop."""
    call_count = 0

    def boom(*args, **kwargs):
        nonlocal call_count
        call_count += 1
        raise OSError("network error")

    with patch("app.email_service.smtplib.SMTP", boom):
        svc.send_order_confirmation(user_id=1, order_id=1)

    assert call_count == 1

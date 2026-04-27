import smtplib
import logging
from email.mime.text import MIMEText
from .config import settings

log = logging.getLogger(__name__)


class EmailService:
    def send_order_confirmation(self, user_id: int, order_id: int) -> bool:
        return self._send(
            to=f"user-{user_id}@ecommerce.local",
            subject=f"Order #{order_id} confirmed",
            body=f"Your order #{order_id} has been confirmed and is being processed.",
        )

    def send_tracking_info(self, user_id: int, order_id: int, tracking_number: str) -> bool:
        return self._send(
            to=f"user-{user_id}@ecommerce.local",
            subject=f"Order #{order_id} shipped",
            body=f"Your order #{order_id} has shipped. Tracking: {tracking_number}",
        )

    def send_order_failed(self, user_id: int, order_id: int) -> bool:
        return self._send(
            to=f"user-{user_id}@ecommerce.local",
            subject=f"Order #{order_id} could not be processed",
            body=f"We're sorry, order #{order_id} could not be fulfilled. You have not been charged.",
        )

    def _send(self, to: str, subject: str, body: str) -> bool:
        try:
            msg = MIMEText(body)
            msg["Subject"] = subject
            msg["From"] = settings.smtp_from
            msg["To"] = to
            with smtplib.SMTP(settings.smtp_host, settings.smtp_port) as smtp:
                smtp.sendmail(settings.smtp_from, [to], msg.as_string())
            log.info("Email sent to %s: %s", to, subject)
            return True
        except Exception as exc:
            log.error("Email failed to %s: %s", to, exc)
            return False


email_service = EmailService()

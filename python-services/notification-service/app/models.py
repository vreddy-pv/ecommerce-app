from datetime import datetime
from sqlalchemy import Column, BigInteger, String, DateTime, func
from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    pass


class NotificationLog(Base):
    __tablename__ = "notification_log"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, nullable=False)
    order_id = Column(BigInteger, nullable=True)
    type = Column(String(50), nullable=False)   # EMAIL, SMS, PUSH
    status = Column(String(20), nullable=False)  # SENT, FAILED
    sent_at = Column(DateTime, default=datetime.utcnow, server_default=func.now())

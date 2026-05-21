"""
Structured logging configuration for the agent.
Logs to both console (dev) and file (prod) with JSON formatting for aggregation.
"""
import logging
import json
import sys
from datetime import datetime
from pathlib import Path


class JSONFormatter(logging.Formatter):
    """Format logs as JSON for easy parsing and aggregation."""

    def format(self, record: logging.LogRecord) -> str:
        """Convert log record to JSON."""
        log_data = {
            "timestamp": datetime.utcnow().isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "module": record.module,
            "function": record.funcName,
            "line": record.lineno,
        }

        # Add extra fields if present
        if hasattr(record, "session_id"):
            log_data["session_id"] = record.session_id
        if hasattr(record, "user_id"):
            log_data["user_id"] = record.user_id
        if hasattr(record, "tool_name"):
            log_data["tool_name"] = record.tool_name
        if hasattr(record, "duration_ms"):
            log_data["duration_ms"] = record.duration_ms
        if hasattr(record, "error_code"):
            log_data["error_code"] = record.error_code

        # Add exception info if present
        if record.exc_info:
            log_data["exception"] = self.formatException(record.exc_info)

        return json.dumps(log_data)


def setup_logging(log_dir: str = "/var/log/agent", level: str = "INFO") -> None:
    """
    Configure structured logging for the agent.

    Args:
        log_dir: Directory to write log files (create if doesn't exist)
        level: Logging level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
    """
    # Create log directory
    Path(log_dir).mkdir(parents=True, exist_ok=True)

    # Set root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(getattr(logging, level))

    # Remove existing handlers
    for handler in root_logger.handlers[:]:
        root_logger.removeHandler(handler)

    # JSON formatter
    json_formatter = JSONFormatter()

    # ── Console Handler (development) ────────────────────────────────
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.DEBUG)
    console_handler.setFormatter(json_formatter)
    root_logger.addHandler(console_handler)

    # ── File Handler (production logs) ────────────────────────────────
    file_handler = logging.FileHandler(f"{log_dir}/agent.log")
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(json_formatter)
    root_logger.addHandler(file_handler)

    # ── Error File Handler (errors only) ─────────────────────────────
    error_handler = logging.FileHandler(f"{log_dir}/agent-errors.log")
    error_handler.setLevel(logging.ERROR)
    error_handler.setFormatter(json_formatter)
    root_logger.addHandler(error_handler)

    # Suppress verbose library logs
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("anthropic").setLevel(logging.INFO)
    logging.getLogger("redis").setLevel(logging.WARNING)

    root_logger.info("Logging configured", extra={"log_dir": log_dir, "level": level})


def get_logger(name: str) -> logging.LoggerAdapter:
    """Get a logger with structured context support."""
    return logging.getLogger(name)


class StructuredLogger:
    """Wrapper for structured logging with context."""

    def __init__(self, logger: logging.Logger):
        self.logger = logger

    def info(self, message: str, **context):
        """Log info with context."""
        self.logger.info(message, extra=context)

    def warning(self, message: str, **context):
        """Log warning with context."""
        self.logger.warning(message, extra=context)

    def error(self, message: str, **context):
        """Log error with context."""
        self.logger.error(message, extra=context, exc_info=True)

    def debug(self, message: str, **context):
        """Log debug with context."""
        self.logger.debug(message, extra=context)

    def chat_message(self, session_id: str, role: str, message: str, tokens: int = 0):
        """Log a chat message."""
        self.info(
            f"{role.upper()} message",
            session_id=session_id,
            role=role,
            message_length=len(message),
            tokens=tokens,
        )

    def tool_call(self, session_id: str, tool_name: str, input_data: dict, duration_ms: int):
        """Log a tool call."""
        self.info(
            f"Tool executed: {tool_name}",
            session_id=session_id,
            tool_name=tool_name,
            input_keys=list(input_data.keys()),
            duration_ms=duration_ms,
        )

    def fraud_detection(
        self, session_id: str, order_id: str, fraud_score: float, risk_level: str
    ):
        """Log fraud detection result."""
        self.info(
            "Fraud detection",
            session_id=session_id,
            order_id=order_id,
            fraud_score=fraud_score,
            risk_level=risk_level,
        )

    def approval_gate(self, session_id: str, tool_name: str, approved: bool):
        """Log approval gate decision."""
        self.info(
            f"Approval gate: {tool_name}",
            session_id=session_id,
            tool_name=tool_name,
            approved=approved,
        )


# Module-level logger
_logger = StructuredLogger(get_logger(__name__))


def agent_logger() -> StructuredLogger:
    """Get the agent logger."""
    return _logger

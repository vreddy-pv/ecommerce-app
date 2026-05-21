"""
Prometheus metrics collection for the agent.
Tracks request counts, response times, errors, tool usage, fraud detection, etc.
Expose via /metrics endpoint (Prometheus-compatible).
"""
import time
from functools import wraps
from typing import Callable, Any, Optional
from collections import defaultdict


class Metrics:
    """Simple in-memory metrics (use Prometheus client for production)."""

    def __init__(self):
        # Request metrics
        self.chat_requests = 0
        self.chat_errors = 0
        self.chat_approvals = 0
        self.chat_rejections = 0

        # Response time tracking (ms)
        self.chat_response_times: list[float] = []

        # Tool metrics
        self.tool_calls: dict[str, int] = defaultdict(int)
        self.tool_errors: dict[str, int] = defaultdict(int)
        self.tool_execution_times: dict[str, list[float]] = defaultdict(list)

        # Fraud detection metrics
        self.fraud_checks = 0
        self.high_risk_detected = 0
        self.medium_risk_detected = 0

        # Session metrics
        self.active_sessions = set()
        self.total_messages = 0

        # Guard metrics
        self.pii_masked = 0
        self.injection_blocked = 0
        self.messages_truncated = 0

    def record_chat_request(self):
        """Record a chat request."""
        self.chat_requests += 1

    def record_chat_error(self):
        """Record a chat error."""
        self.chat_errors += 1

    def record_chat_response_time(self, duration_ms: float):
        """Record chat response time."""
        self.chat_response_times.append(duration_ms)

    def record_approval(self, approved: bool):
        """Record approval/rejection."""
        if approved:
            self.chat_approvals += 1
        else:
            self.chat_rejections += 1

    def record_tool_call(self, tool_name: str, duration_ms: float, error: bool = False):
        """Record a tool call."""
        self.tool_calls[tool_name] += 1
        self.tool_execution_times[tool_name].append(duration_ms)
        if error:
            self.tool_errors[tool_name] += 1

    def record_fraud_check(self, fraud_score: float):
        """Record fraud detection."""
        self.fraud_checks += 1
        if fraud_score > 70:
            self.high_risk_detected += 1
        elif fraud_score > 30:
            self.medium_risk_detected += 1

    def record_pii_masked(self, count: int):
        """Record PII masking."""
        self.pii_masked += count

    def record_injection_blocked(self):
        """Record prompt injection block."""
        self.injection_blocked += 1

    def record_message_truncated(self):
        """Record message truncation."""
        self.messages_truncated += 1

    def record_session(self, session_id: str):
        """Track active session."""
        self.active_sessions.add(session_id)

    def record_message(self):
        """Record a message."""
        self.total_messages += 1

    def get_averages(self) -> dict[str, float]:
        """Get average metrics."""
        return {
            "avg_chat_response_time_ms": (
                sum(self.chat_response_times) / len(self.chat_response_times)
                if self.chat_response_times
                else 0
            ),
            "avg_tool_response_times_ms": {
                tool: sum(times) / len(times) if times else 0
                for tool, times in self.tool_execution_times.items()
            },
            "high_risk_percentage": (
                (self.high_risk_detected / self.fraud_checks * 100)
                if self.fraud_checks > 0
                else 0
            ),
        }

    def to_dict(self) -> dict[str, Any]:
        """Serialize metrics to dictionary."""
        avgs = self.get_averages()
        return {
            "requests": {
                "total": self.chat_requests,
                "errors": self.chat_errors,
                "error_rate": (
                    (self.chat_errors / self.chat_requests * 100)
                    if self.chat_requests > 0
                    else 0
                ),
            },
            "response_times": {
                "average_ms": avgs["avg_chat_response_time_ms"],
                "samples": len(self.chat_response_times),
                "min_ms": min(self.chat_response_times) if self.chat_response_times else 0,
                "max_ms": max(self.chat_response_times) if self.chat_response_times else 0,
            },
            "approvals": {
                "approved": self.chat_approvals,
                "rejected": self.chat_rejections,
                "total": self.chat_approvals + self.chat_rejections,
            },
            "tools": {
                "calls_by_tool": dict(self.tool_calls),
                "errors_by_tool": dict(self.tool_errors),
                "avg_execution_times_ms": avgs["avg_tool_response_times_ms"],
            },
            "fraud_detection": {
                "total_checks": self.fraud_checks,
                "high_risk": self.high_risk_detected,
                "medium_risk": self.medium_risk_detected,
                "high_risk_percentage": avgs["high_risk_percentage"],
            },
            "sessions": {
                "active": len(self.active_sessions),
                "total_messages": self.total_messages,
            },
            "guardrails": {
                "pii_masked": self.pii_masked,
                "injections_blocked": self.injection_blocked,
                "messages_truncated": self.messages_truncated,
            },
        }

    def reset(self):
        """Reset all metrics (careful!)."""
        self.__init__()


# Global metrics instance
_metrics = Metrics()


def get_metrics() -> Metrics:
    """Get the global metrics instance."""
    return _metrics


def track_time(func: Callable) -> Callable:
    """Decorator to track function execution time."""

    @wraps(func)
    async def async_wrapper(*args: Any, **kwargs: Any) -> Any:
        start = time.perf_counter()
        try:
            result = await func(*args, **kwargs)
            return result
        finally:
            duration_ms = (time.perf_counter() - start) * 1000
            # Use logger to record timing
            from .logging_config import get_logger

            logger = get_logger(__name__)
            logger.debug(f"{func.__name__} took {duration_ms:.2f}ms")

    @wraps(func)
    def sync_wrapper(*args: Any, **kwargs: Any) -> Any:
        start = time.perf_counter()
        try:
            result = func(*args, **kwargs)
            return result
        finally:
            duration_ms = (time.perf_counter() - start) * 1000
            from .logging_config import get_logger

            logger = get_logger(__name__)
            logger.debug(f"{func.__name__} took {duration_ms:.2f}ms")

    # Return appropriate wrapper based on function type
    if hasattr(func, "__await__"):
        return async_wrapper
    return sync_wrapper

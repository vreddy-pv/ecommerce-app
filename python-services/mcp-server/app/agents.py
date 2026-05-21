"""
Autonomous agent for e-commerce operations.
Simpler implementation without LangGraph for now (to avoid dependency issues).
Uses Claude API directly with tool use pattern.
"""
import json
import uuid
import asyncio
from typing import Any, Optional
from datetime import datetime
import anthropic

from .config import settings


class AgentState:
    """Conversation state for multi-turn agent execution."""

    def __init__(self, session_id: str):
        self.session_id = session_id
        self.messages: list[dict] = []
        self.requires_approval = False
        self.pending_action: dict[str, Any] | None = None
        self.step_count = 0
        self.created_at = datetime.utcnow().isoformat()

    def to_dict(self) -> dict:
        return {
            "session_id": self.session_id,
            "messages": self.messages,
            "requires_approval": self.requires_approval,
            "pending_action": self.pending_action,
            "step_count": self.step_count,
            "created_at": self.created_at,
        }


# Global session store (in production, use Redis)
_sessions: dict[str, AgentState] = {}


def get_or_create_session(session_id: str | None = None) -> AgentState:
    """Get existing session or create new one."""
    if session_id is None:
        session_id = str(uuid.uuid4())
    if session_id not in _sessions:
        _sessions[session_id] = AgentState(session_id)
    return _sessions[session_id]


def _get_tools_schema() -> list[dict]:
    """Build Claude tool schema from agent_tools."""
    return [
        {
            "name": "get_order_details",
            "description": "Retrieve full details for a specific order including items, status, customer info",
            "input_schema": {
                "type": "object",
                "properties": {"order_id": {"type": "string", "description": "Order ID"}},
                "required": ["order_id"],
            },
        },
        {
            "name": "get_recent_orders",
            "description": "Retrieve recent orders optionally filtered by user_id",
            "input_schema": {
                "type": "object",
                "properties": {
                    "user_id": {"type": "string", "description": "User ID (optional)"},
                    "limit": {"type": "integer", "description": "Max results (default: 10)"},
                },
            },
        },
        {
            "name": "check_fraud_score",
            "description": "Get fraud risk score (0-100) and risk factors for an order. HIGH RISK: > 70",
            "input_schema": {
                "type": "object",
                "properties": {"order_id": {"type": "string", "description": "Order ID"}},
                "required": ["order_id"],
            },
        },
        {
            "name": "check_inventory_levels",
            "description": "Check inventory levels for a product or return low-stock alerts",
            "input_schema": {
                "type": "object",
                "properties": {"product_id": {"type": "string", "description": "Product ID (optional)"}},
            },
        },
        {
            "name": "flag_for_review",
            "description": "Flag an order for manual review (no approval required)",
            "input_schema": {
                "type": "object",
                "properties": {
                    "order_id": {"type": "string", "description": "Order ID"},
                    "reason": {"type": "string", "description": "Reason for flag"},
                    "risk_score": {"type": "number", "description": "Risk score (optional)"},
                },
                "required": ["order_id", "reason"],
            },
        },
        {
            "name": "cancel_order",
            "description": "Cancel an order. REQUIRES USER APPROVAL.",
            "input_schema": {
                "type": "object",
                "properties": {
                    "order_id": {"type": "string", "description": "Order ID"},
                    "reason": {"type": "string", "description": "Cancellation reason"},
                },
                "required": ["order_id"],
            },
        },
        {
            "name": "update_inventory",
            "description": "Update stock level for a product. REQUIRES USER APPROVAL.",
            "input_schema": {
                "type": "object",
                "properties": {
                    "product_id": {"type": "string", "description": "Product ID"},
                    "quantity": {"type": "integer", "description": "New absolute stock count"},
                    "reason": {"type": "string", "description": "Reason for update"},
                },
                "required": ["product_id", "quantity"],
            },
        },
    ]


async def run_agent(state: AgentState) -> str:
    """
    Run the agent reasoning loop until completion or approval needed.
    Returns the final agent response.
    """
    client = anthropic.Anthropic(api_key=settings.anthropic_api_key)

    system_prompt = """You are an e-commerce AI agent helping manage orders and inventory.
Your tasks:
- Analyze orders and detect fraud
- Check inventory levels
- Flag suspicious orders for human review
- Execute safe operations (flagging)
- Request approval for sensitive operations (cancel, update inventory)

When fraud score > 70, flag for review before taking other actions.
When you need user approval (cancel_order, update_inventory), explain why and ask for confirmation.
Be concise and factual."""

    max_loops = 5
    loop_count = 0

    while loop_count < max_loops:
        loop_count += 1
        state.step_count = loop_count

        # Call Claude with tool schema
        response = client.messages.create(
            model="claude-opus-4-7",
            max_tokens=1024,
            system=system_prompt,
            tools=_get_tools_schema(),
            messages=state.messages,
        )

        # Add assistant response to history
        state.messages.append({"role": "assistant", "content": response.content})

        # Check for stop reason
        if response.stop_reason == "end_turn":
            # Extract text response
            for block in response.content:
                if hasattr(block, "text"):
                    return block.text
            return "Agent completed."

        # Process tool calls
        has_tool_calls = any(block.type == "tool_use" for block in response.content)
        if not has_tool_calls:
            # No more tools, return final response
            for block in response.content:
                if hasattr(block, "text"):
                    return block.text
            return "Agent completed."

        # Execute tools
        from . import agent_tools

        tool_results = []

        for block in response.content:
            if block.type == "tool_use":
                tool_name = block.name
                tool_input = block.input

                # Check if sensitive operation (requires approval)
                if tool_name in ["cancel_order", "update_inventory"]:
                    state.requires_approval = True
                    state.pending_action = {
                        "tool": tool_name,
                        "input": tool_input,
                        "tool_call_id": block.id,
                    }
                    tool_results.append(
                        {
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": f"[APPROVAL REQUIRED] Waiting for user to confirm {tool_name}",
                        }
                    )
                    continue

                # Execute the tool
                try:
                    tool_func = getattr(agent_tools, tool_name, None)
                    if tool_func is None:
                        result = f"Tool {tool_name} not found"
                    else:
                        result = await tool_func(**tool_input)

                    tool_results.append(
                        {
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": json.dumps(result) if isinstance(result, dict) else str(result),
                        }
                    )
                except Exception as e:
                    tool_results.append(
                        {
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": f"Error: {str(e)}",
                            "is_error": True,
                        }
                    )

        # If approval needed, stop here and return
        if state.requires_approval:
            # Get the assistant's text response
            for block in response.content:
                if hasattr(block, "text"):
                    return block.text
            return "Awaiting your approval to proceed."

        # Add tool results and continue loop
        state.messages.append({"role": "user", "content": tool_results})

    return "Agent reached max iterations."

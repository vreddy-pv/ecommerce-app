"""
FastAPI endpoints for the autonomous agent chat interface.
Two endpoints: POST /chat (normal turn) and POST /chat/approve (approval flow).
Uses pluggable session store (in-memory or Redis).
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import json
from typing import Optional

from .session_store import get_or_create_session, save_session, get_session_store
from .agents import run_agent
from .guardrails import apply_input_guardrails, apply_output_guardrails


app = FastAPI(title="E-Commerce AI Agent")


class ChatRequest(BaseModel):
    """User message for the agent."""

    session_id: Optional[str] = None
    message: str


class ChatApprovalRequest(BaseModel):
    """Approval for a sensitive operation."""

    session_id: str
    approve: bool
    reason: Optional[str] = None


class ChatResponse(BaseModel):
    """Agent response with optional approval gate status."""

    session_id: str
    response: str
    requires_approval: bool = False
    pending_action: Optional[dict] = None
    step_count: int
    messages_in_session: int


@app.post("/chat")
async def chat(req: ChatRequest) -> ChatResponse:
    """
    Main chat endpoint.
    Accepts user message, runs agent loop, returns response.
    If agent detects sensitive operation, sets requires_approval=true.
    """
    # Get or create session
    session = await get_or_create_session(req.session_id)

    # Apply input guardrails
    sanitized_message, warnings = apply_input_guardrails(req.message)
    if warnings:
        print(f"Input guard warnings: {warnings}")

    # Add user message to history
    session.messages.append({"role": "user", "content": sanitized_message})

    # Run the agent
    try:
        response = await run_agent(session)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Agent error: {str(e)}")

    # Apply output guardrails
    sanitized_response = apply_output_guardrails(response)

    # Save session to store (Redis or in-memory)
    await save_session(session)

    return ChatResponse(
        session_id=session.session_id,
        response=sanitized_response,
        requires_approval=session.requires_approval,
        pending_action=session.pending_action,
        step_count=session.step_count,
        messages_in_session=len(session.messages),
    )


@app.post("/chat/approve")
async def chat_approve(req: ChatApprovalRequest) -> ChatResponse:
    """
    Approval flow endpoint.
    User approves or rejects pending action from approval gate.
    If approved, executes action and resumes agent reasoning.
    """
    session = await get_or_create_session(req.session_id)

    if not session.requires_approval:
        raise HTTPException(status_code=400, detail="No approval pending for this session")

    if not session.pending_action:
        raise HTTPException(status_code=400, detail="No pending action found")

    # Execute the pending action if approved
    if req.approve:
        from . import agent_tools

        tool_name = session.pending_action["tool"]
        tool_input = session.pending_action["input"]

        try:
            tool_func = getattr(agent_tools, tool_name, None)
            if tool_func is None:
                raise ValueError(f"Tool {tool_name} not found")

            result = await tool_func(**tool_input)

            # Add result to messages and clear approval state
            session.messages.append(
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "tool_result",
                            "tool_use_id": session.pending_action.get("tool_call_id", "unknown"),
                            "content": json.dumps(result) if isinstance(result, dict) else str(result),
                        }
                    ],
                }
            )
        except Exception as e:
            session.messages.append(
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "tool_result",
                            "tool_use_id": session.pending_action.get("tool_call_id", "unknown"),
                            "content": f"Error executing {tool_name}: {str(e)}",
                            "is_error": True,
                        }
                    ],
                }
            )
    else:
        # Rejection
        session.messages.append(
            {
                "role": "user",
                "content": f"User rejected: {session.pending_action['tool']}. Reason: {req.reason or 'None given'}",
            }
        )

    # Clear approval state
    session.requires_approval = False
    session.pending_action = None

    # Resume agent reasoning
    try:
        response = await run_agent(session)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Agent error: {str(e)}")

    # Apply output guardrails
    sanitized_response = apply_output_guardrails(response)

    # Save session to store
    await save_session(session)

    return ChatResponse(
        session_id=session.session_id,
        response=sanitized_response,
        requires_approval=session.requires_approval,
        pending_action=session.pending_action,
        step_count=session.step_count,
        messages_in_session=len(session.messages),
    )


@app.get("/session/{session_id}")
async def get_session_info(session_id: str) -> dict:
    """Get session state for debugging."""
    store = get_session_store()
    session = await store.get(session_id)

    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    return session.to_dict()


@app.delete("/session/{session_id}")
async def delete_session(session_id: str) -> dict:
    """Delete session (cleanup)."""
    store = get_session_store()
    await store.delete(session_id)
    return {"status": "deleted", "session_id": session_id}

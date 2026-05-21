/**
 * Agent API client — calls the autonomous AI agent endpoints.
 * Uses the existing axios client (with Keycloak auth).
 */
import client from './client'

export interface ChatRequest {
  session_id?: string | null
  message: string
}

export interface PendingAction {
  tool: string
  input: Record<string, any>
  tool_call_id: string
}

export interface ChatResponse {
  session_id: string
  response: string
  requires_approval: boolean
  pending_action: PendingAction | null
  step_count: number
  messages_in_session: number
}

export interface ApprovalRequest {
  session_id: string
  approve: boolean
  reason?: string
}

/**
 * Send a message to the agent.
 * Returns the agent's response. If requires_approval=true, show dialog and call approveAction.
 */
export const chat = async (req: ChatRequest): Promise<ChatResponse> => {
  const response = await client.post<ChatResponse>('/ai/chat', req)
  return response.data
}

/**
 * Approve or reject a pending sensitive operation.
 * Agent resumes reasoning and returns final response.
 */
export const approveAction = async (req: ApprovalRequest): Promise<ChatResponse> => {
  const response = await client.post<ChatResponse>('/ai/chat/approve', req)
  return response.data
}

/**
 * Get session state (for debugging).
 */
export const getSession = async (sessionId: string): Promise<any> => {
  const response = await client.get(`/ai/session/${sessionId}`)
  return response.data
}

/**
 * Clear a session.
 */
export const deleteSession = async (sessionId: string): Promise<any> => {
  const response = await client.delete(`/ai/session/${sessionId}`)
  return response.data
}

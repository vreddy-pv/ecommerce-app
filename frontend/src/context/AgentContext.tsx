/**
 * AgentContext — manages agent chat state, session, and approval flow.
 * Use with useAgent hook in components.
 */
import React, { createContext, useContext, useState, useCallback } from 'react'
import { chat, approveAction, ChatResponse, PendingAction } from '../api/agent'

interface Message {
  id: string
  role: 'user' | 'agent'
  content: string
  timestamp: Date
}

interface AgentContextType {
  sessionId: string | null
  messages: Message[]
  isLoading: boolean
  requiresApproval: boolean
  pendingAction: PendingAction | null
  error: string | null

  sendMessage: (text: string) => Promise<void>
  approveAction: (approve: boolean, reason?: string) => Promise<void>
  clearError: () => void
  resetSession: () => void
}

const AgentContext = createContext<AgentContextType | undefined>(undefined)

export const AgentProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [requiresApproval, setRequiresApproval] = useState(false)
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null)
  const [error, setError] = useState<string | null>(null)

  const sendMessage = useCallback(
    async (text: string) => {
      if (!text.trim()) return

      setIsLoading(true)
      setError(null)

      try {
        // Add user message to UI
        const userMsgId = `msg-${Date.now()}`
        setMessages((prev) => [
          ...prev,
          {
            id: userMsgId,
            role: 'user',
            content: text,
            timestamp: new Date(),
          },
        ])

        // Call agent API
        const response = await chat({
          session_id: sessionId,
          message: text,
        })

        // Update session ID if new
        if (!sessionId) {
          setSessionId(response.session_id)
        }

        // Add agent response to UI
        const agentMsgId = `msg-${Date.now()}`
        setMessages((prev) => [
          ...prev,
          {
            id: agentMsgId,
            role: 'agent',
            content: response.response,
            timestamp: new Date(),
          },
        ])

        // Handle approval gate
        if (response.requires_approval) {
          setRequiresApproval(true)
          setPendingAction(response.pending_action)
        } else {
          setRequiresApproval(false)
          setPendingAction(null)
        }
      } catch (err) {
        const errMsg = err instanceof Error ? err.message : 'Failed to send message'
        setError(errMsg)
        console.error('Agent error:', err)
      } finally {
        setIsLoading(false)
      }
    },
    [sessionId]
  )

  const handleApproveAction = useCallback(
    async (approve: boolean, reason?: string) => {
      if (!sessionId) {
        setError('No active session')
        return
      }

      setIsLoading(true)
      setError(null)

      try {
        // Call approval endpoint
        const response = await approveAction({
          session_id: sessionId,
          approve,
          reason,
        })

        // Add approval action to messages
        const actionMsg = approve
          ? `✅ Approved: ${pendingAction?.tool || 'Action'}`
          : `❌ Rejected: ${pendingAction?.tool || 'Action'}${reason ? ` - ${reason}` : ''}`

        setMessages((prev) => [
          ...prev,
          {
            id: `msg-${Date.now()}`,
            role: 'user',
            content: actionMsg,
            timestamp: new Date(),
          },
        ])

        // Add agent's final response
        const agentMsgId = `msg-${Date.now()}`
        setMessages((prev) => [
          ...prev,
          {
            id: agentMsgId,
            role: 'agent',
            content: response.response,
            timestamp: new Date(),
          },
        ])

        // Clear approval state
        setRequiresApproval(false)
        setPendingAction(null)
      } catch (err) {
        const errMsg = err instanceof Error ? err.message : 'Failed to process approval'
        setError(errMsg)
        console.error('Approval error:', err)
      } finally {
        setIsLoading(false)
      }
    },
    [sessionId, pendingAction]
  )

  const clearError = useCallback(() => {
    setError(null)
  }, [])

  const resetSession = useCallback(() => {
    setSessionId(null)
    setMessages([])
    setRequiresApproval(false)
    setPendingAction(null)
    setError(null)
  }, [])

  const value: AgentContextType = {
    sessionId,
    messages,
    isLoading,
    requiresApproval,
    pendingAction,
    error,
    sendMessage,
    approveAction: handleApproveAction,
    clearError,
    resetSession,
  }

  return <AgentContext.Provider value={value}>{children}</AgentContext.Provider>
}

export const useAgent = (): AgentContextType => {
  const context = useContext(AgentContext)
  if (!context) {
    throw new Error('useAgent must be used within AgentProvider')
  }
  return context
}

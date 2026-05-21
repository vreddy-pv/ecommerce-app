/**
 * AgentChat — Displays the agent conversation and handles message input.
 * Shows approval dialog when requires_approval=true.
 */
import React, { useRef, useEffect, useState } from 'react'
import { useAgent } from '../context/AgentContext'
import styles from './AgentChat.module.css'

export const AgentChat: React.FC = () => {
  const { messages, isLoading, requiresApproval, pendingAction, error, sendMessage, approveAction, clearError } =
    useAgent()
  const [input, setInput] = useState('')
  const [rejectionReason, setRejectionReason] = useState('')
  const [showRejectionInput, setShowRejectionInput] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Auto-scroll to latest message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!input.trim() || isLoading) return

    await sendMessage(input)
    setInput('')
  }

  const handleApprove = async (approve: boolean) => {
    await approveAction(approve, rejectionReason)
    setRejectionReason('')
    setShowRejectionInput(false)
  }

  return (
    <div className={styles.chatContainer}>
      {/* Header */}
      <div className={styles.header}>
        <h2>🤖 E-Commerce AI Agent</h2>
        <p className={styles.subtitle}>Analyze orders, detect fraud, manage inventory</p>
      </div>

      {/* Messages */}
      <div className={styles.messagesBox}>
        {messages.length === 0 && (
          <div className={styles.welcome}>
            <p>👋 Welcome! Ask me about:</p>
            <ul>
              <li>Order fraud detection</li>
              <li>Inventory levels</li>
              <li>Customer order history</li>
              <li>System health</li>
            </ul>
          </div>
        )}

        {messages.map((msg) => (
          <div key={msg.id} className={`${styles.message} ${styles[msg.role]}`}>
            <div className={styles.messageContent}>
              <span className={styles.role}>{msg.role === 'user' ? 'You' : 'Agent'}</span>
              <p>{msg.content}</p>
              <span className={styles.timestamp}>{msg.timestamp.toLocaleTimeString()}</span>
            </div>
          </div>
        ))}

        {isLoading && (
          <div className={`${styles.message} ${styles.agent}`}>
            <div className={styles.messageContent}>
              <span className={styles.role}>Agent</span>
              <div className={styles.typing}>
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Error */}
      {error && (
        <div className={styles.errorBanner}>
          <span>⚠️ {error}</span>
          <button onClick={clearError} className={styles.closeBtn}>
            ×
          </button>
        </div>
      )}

      {/* Approval Dialog */}
      {requiresApproval && pendingAction && (
        <div className={styles.approvalDialog}>
          <div className={styles.approvalContent}>
            <h3>⚠️ Approval Required</h3>
            <p>The agent wants to execute a sensitive operation:</p>

            <div className={styles.actionDetails}>
              <p className={styles.actionName}>
                <strong>Operation:</strong> {pendingAction.tool}
              </p>
              <div className={styles.actionInput}>
                <strong>Details:</strong>
                <pre>{JSON.stringify(pendingAction.input, null, 2)}</pre>
              </div>
            </div>

            <p className={styles.confirmText}>Do you want to approve this action?</p>

            <div className={styles.approvalButtons}>
              <button
                onClick={() => handleApprove(true)}
                disabled={isLoading}
                className={`${styles.btn} ${styles.btnPrimary}`}
              >
                ✅ Approve
              </button>

              <button
                onClick={() => setShowRejectionInput(!showRejectionInput)}
                disabled={isLoading}
                className={`${styles.btn} ${styles.btnSecondary}`}
              >
                ❌ Reject
              </button>
            </div>

            {showRejectionInput && (
              <div className={styles.rejectionInput}>
                <textarea
                  placeholder="Why are you rejecting this action? (optional)"
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  rows={2}
                />
                <button
                  onClick={() => handleApprove(false)}
                  disabled={isLoading}
                  className={`${styles.btn} ${styles.btnDanger}`}
                >
                  Reject
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Input */}
      <form onSubmit={handleSendMessage} className={styles.inputForm}>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask about orders, fraud, inventory..."
          disabled={isLoading || requiresApproval}
          className={styles.input}
        />
        <button
          type="submit"
          disabled={isLoading || !input.trim() || requiresApproval}
          className={styles.sendBtn}
        >
          Send →
        </button>
      </form>
    </div>
  )
}

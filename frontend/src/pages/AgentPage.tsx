/**
 * AgentPage — The AI agent chat interface.
 * Can be accessed at /agent or /ai-assistant
 */
import React from 'react'
import { AgentProvider } from '../context/AgentContext'
import { AgentChat } from '../components/AgentChat'

export const AgentPage: React.FC = () => {
  return (
    <AgentProvider>
      <AgentChat />
    </AgentProvider>
  )
}

export default AgentPage

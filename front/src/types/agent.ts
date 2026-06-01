export type AgentMode = 'chat' | 'memory' | 'specific'

export interface AgentRequest {
  conversationId?: string
  message: string
  tools?: string[]
  thinkingMode?: boolean
}

export interface AgentResponse {
  conversationId?: string
  reply?: string
  model?: string
  agentType?: string
  traceId?: string
  thinking?: string
}

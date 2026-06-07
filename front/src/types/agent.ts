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
  answer?: string
  result?: string
  directAnswer?: string
  finalAnswer?: string
  model?: string
  agentType?: string
  traceId?: string
  thinking?: string
  totalTimeMs?: number
  code?: number
  message?: string
}

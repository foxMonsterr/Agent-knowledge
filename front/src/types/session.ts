export interface SessionHistoryItem {
  role: string
  content: string
  createdAt?: string
}

export interface ChatHistoryEntity {
  id: number
  conversationId: string
  username: string
  messageRole: string
  content: string
  agentType?: string
  model?: string
  promptTokens?: number
  completionTokens?: number
  latencyMs?: number
  createdAt?: string
}

export type ChatMode = 'simple' | 'memory' | 'expert'

export interface ChatRequest {
  conversationId?: string
  message: string
  model?: string
  role?: string
  level?: string
  thinkingMode?: boolean
}

export interface ChatResponse {
  conversationId?: string
  reply?: string
  thinking?: string
  traceId?: string
  model?: string
  historySize?: number
  tokenUsage?: {
    promptTokens?: number
    completionTokens?: number
    totalTokens?: number
  }
}

export interface SessionVO {
  sessionId: string
  title?: string
  summary?: string
  lastMessageAt?: string
  status?: string
}

export interface SessionExportMessage {
  role?: string
  content?: string
  createdAt?: string
  agentType?: string
  model?: string
  promptTokens?: number
  completionTokens?: number
  latencyMs?: number
}

export interface SessionExportResponse {
  conversationId: string
  title?: string
  summary?: string
  status?: string
  createdAt?: string
  updatedAt?: string
  messages?: SessionExportMessage[]
}

export interface ChatMessageVO {
  sessionId?: string
  userId?: string
  role: 'user' | 'assistant' | 'system'
  content: string
  createdAt?: string
}

export interface StructuredRequest {
  input: string
}

export interface StructuredResponse<T> {
  result: T
  outputType: string
  originalInput: string
}

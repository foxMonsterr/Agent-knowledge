import type { ReActSourceRef, ReActStep } from './react'

export type ConversationAgentType =
  | 'chat'
  | 'tool'
  | 'rag'
  | 'planning'
  | 'full'
  | 'learn-react'
  | 'general-react'

export type ConversationMode =
  | 'chat'
  | 'memory'
  | 'specific'
  | 'tools'
  | 'rag'
  | 'rag_manual'
  | 'planning'
  | 'planning_only'
  | 'learn'
  | 'react'

export type ConversationSseEventType =
  | 'start'
  | 'delta'
  | 'message'
  | 'thought'
  | 'action'
  | 'observation'
  | 'source'
  | 'final_answer'
  | 'error'
  | 'done'
  | 'heartbeat'

export interface ConversationRunRequest {
  conversationId?: string
  message: string
  agentType?: ConversationAgentType
  mode?: ConversationMode
  tools?: string[]
  thinkingMode?: boolean
  memoryEnabled?: boolean
  stream?: boolean
  model?: string
  role?: string
  level?: string
  topK?: number
  similarityThreshold?: number
  autoExecute?: boolean
  strategy?: string
  noteIds?: string[]
  tags?: string[]
  category?: string
  maxIterations?: number
  autoCreateNote?: boolean
  metadata?: Record<string, unknown>
}

export interface ConversationRunResponse {
  conversationId: string
  traceId?: string
  agentType: ConversationAgentType
  mode: ConversationMode
  reply: string
  thinking?: string
  model?: string
  memoryEnabled: boolean
  sources?: string[]
  retrievedChunks?: number
  planned?: boolean
  steps?: unknown[]
  totalTimeMs?: number
  status: 'success' | 'failed' | 'stopped'
  metadata?: Record<string, unknown>
}

export interface ConversationSseEvent<T = any> {
  eventId: string
  type: ConversationSseEventType
  conversationId: string
  traceId?: string
  agentType: ConversationAgentType
  mode: ConversationMode
  timestamp: string
  data: T
}

export interface ConversationMessageVO {
  role: 'user' | 'assistant' | 'system'
  content: string
  agentType?: ConversationAgentType
  model?: string
  traceId?: string
  latencyMs?: number
  createdAt: string
}

export interface ConversationSummaryVO {
  conversationId: string
  title: string
  summary: string
  status: 'active' | 'archived'
  agentTypes: ConversationAgentType[]
  lastMessageAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface ConversationStreamState {
  answer: string
  steps: ReActStep[]
  sources: ReActSourceRef[]
}

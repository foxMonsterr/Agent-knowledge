export type ReActStrategy = 'auto' | 'quiz' | 'review' | 'explore'

export interface ReActSourceRef {
  sourceId: string
  sourceType: 'note' | 'document' | 'chunk' | 'manual'
  title: string
  snippet?: string
  score?: number
  noteId?: string
  docId?: string
  chunkId?: string
}

export interface ReActStep {
  stepId?: string
  stepNumber: number
  type: string
  state?: string
  thought?: string
  actionName?: string
  actionInput?: Record<string, unknown>
  observation?: string
  sources?: ReActSourceRef[]
  status?: string
  startedAt?: string
  endedAt?: string
  durationMs?: number
  errorMessage?: string
}

export interface ReActSseEvent<T = any> {
  eventId: string
  type: 'start' | 'delta' | 'message' | 'thought' | 'action' | 'observation' | 'source' | 'final_answer' | 'error' | 'done' | 'heartbeat'
  traceId?: string
  conversationId?: string
  sessionId?: string
  agentType?: string
  mode?: string
  stepNumber?: number
  timestamp: string
  data: T
}

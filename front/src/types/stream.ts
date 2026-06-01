export type StreamMode = 'basic' | 'tools' | 'knowledge' | 'planning' | 'agent'
export type StreamStatus = 'idle' | 'connecting' | 'streaming' | 'completed' | 'stopped' | 'error'

export interface StreamChatParams {
  message: string
  conversationId?: string
  mode?: StreamMode
  endpoint?: string
  query?: Record<string, string | number | boolean | undefined>
}

export interface StreamMessageLog {
  id: string
  time: string
  type: 'user' | 'assistant' | 'status' | 'error'
  content: string
}

export interface StreamEventPayload {
  type: 'start' | 'delta' | 'message' | 'done' | 'error' | 'status'
  content?: string
  raw?: string
}

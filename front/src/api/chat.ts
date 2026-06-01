import request from '@/utils/request'
import type { ChatMessageVO, ChatRequest, ChatResponse, SessionExportResponse, SessionVO } from '@/types/chat'

export const createSession = (data: { title?: string }) => {
  return request.post('/session/create', data) as Promise<SessionVO>
}

export const listSessions = () => {
  return request.get('/session/list') as Promise<SessionVO[]>
}

export const renameSession = (sessionId: string, title: string) => {
  return request.patch('/session/' + encodeURIComponent(sessionId) + '/title', { title }) as Promise<SessionVO>
}

export const exportSession = (sessionId: string) => {
  return request.get('/session/' + encodeURIComponent(sessionId) + '/export') as Promise<SessionExportResponse>
}

export const getSessionMessages = (sessionId: string) => {
  return request.get(`/session/${encodeURIComponent(sessionId)}/history`) as Promise<ChatMessageVO[]>
}

export const sendMemoryChat = (data: ChatRequest) => {
  return request.post('/chat/memory', data) as Promise<ChatResponse>
}

export const streamMemoryChatUrl = (conversationId?: string, message = '') => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const query = new URLSearchParams({ message })
  if (conversationId) query.set('conversationId', conversationId)
  return `${baseUrl.replace(/\/$/, '')}/chat/memory/stream?${query.toString()}`
}

export const queryPing = () => {
  return request.get('/chat/ping') as Promise<string>
}

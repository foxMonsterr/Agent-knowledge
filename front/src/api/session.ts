import request from '@/utils/request'
import type { ChatMessageVO, SessionExportResponse, SessionVO } from '@/types/chat'
import type { SessionHistoryItem } from '@/types/session'
import type { ConversationMessageVO, ConversationSummaryVO } from '@/types/conversation'

const toSessionVO = (item: ConversationSummaryVO): SessionVO => ({
  sessionId: item.conversationId,
  title: item.title,
  summary: item.summary,
  status: item.status,
  lastMessageAt: item.lastMessageAt,
})

const toMessageVO = (item: ConversationMessageVO): ChatMessageVO => ({
  role: item.role,
  content: item.content,
  createdAt: item.createdAt,
})

export const createSession = (data: { title?: string }) => {
  return request.post<ConversationSummaryVO>('/conversations', data).then(toSessionVO)
}

export const listSessions = () => {
  return request.get<ConversationSummaryVO[]>('/conversations').then((list) => (list || []).map(toSessionVO))
}

export const renameSession = (sessionId: string, title: string) => {
  return request.patch<ConversationSummaryVO>('/conversations/' + encodeURIComponent(sessionId) + '/title', { title })
    .then(toSessionVO)
}

export const exportSession = (sessionId: string) => {
  return request.get('/session/' + encodeURIComponent(sessionId) + '/export') as Promise<SessionExportResponse>
}

export const getSessionMessages = (sessionId: string) => {
  return request.get<ConversationMessageVO[]>(`/conversations/${encodeURIComponent(sessionId)}/messages`)
    .then((list) => (list || []).map(toMessageVO))
}

export const getSessionHistory = (conversationId: string) => {
  return request.get<ConversationMessageVO[]>(`/conversations/${encodeURIComponent(conversationId)}/messages`)
    .then((list) => (list || []).map((item) => ({
      role: item.role,
      content: item.content,
      createdAt: item.createdAt,
      agentType: item.agentType,
      model: item.model,
      traceId: item.traceId,
      latencyMs: item.latencyMs,
    } as SessionHistoryItem)))
}

export const clearSession = (conversationId: string) => {
  return request.delete<string>(`/conversations/${encodeURIComponent(conversationId)}`)
}

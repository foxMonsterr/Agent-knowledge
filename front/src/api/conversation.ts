import request from '@/utils/request'
import type { ConversationRunRequest, ConversationRunResponse } from '@/types/conversation'

export interface ConversationStreamParams extends ConversationRunRequest {
  endpoint?: string
}

export const sendConversationChat = (data: ConversationRunRequest) => {
  return request.post<ConversationRunResponse>('/conversations/chat', data)
}

export const conversationStreamUrl = (params: ConversationStreamParams) => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const query = new URLSearchParams()
  query.set('message', params.message)
  if (params.conversationId) query.set('conversationId', params.conversationId)
  if (params.agentType) query.set('agentType', params.agentType)
  if (params.mode) query.set('mode', params.mode)
  if (params.tools?.length) query.set('tools', params.tools.join(','))
  if (params.thinkingMode !== undefined) query.set('thinkingMode', String(params.thinkingMode))
  if (params.memoryEnabled !== undefined) query.set('memoryEnabled', String(params.memoryEnabled))
  if (params.topK !== undefined) query.set('topK', String(params.topK))
  if (params.similarityThreshold !== undefined) query.set('similarityThreshold', String(params.similarityThreshold))
  if (params.autoExecute !== undefined) query.set('autoExecute', String(params.autoExecute))
  if (params.strategy) query.set('strategy', params.strategy)
  const endpoint = params.endpoint || '/conversations/chat/stream'
  return `${baseUrl.replace(/\/$/, '')}${endpoint}?${query.toString()}`
}

export const listConversations = (params?: { agentType?: string; keyword?: string }) => {
  return request.get('/conversations', { params })
}

export const getConversationMessages = (conversationId: string) => {
  return request.get(`/conversations/${encodeURIComponent(conversationId)}/messages`)
}

export const createConversation = (data?: { title?: string }) => {
  return request.post('/conversations', data || {})
}

export const renameConversation = (conversationId: string, title: string) => {
  return request.patch(`/conversations/${encodeURIComponent(conversationId)}/title`, { title })
}

export const clearConversationMemory = (conversationId: string) => {
  return request.delete(`/conversations/${encodeURIComponent(conversationId)}/memory`)
}

export const deleteConversation = (conversationId: string) => {
  return request.delete(`/conversations/${encodeURIComponent(conversationId)}`)
}

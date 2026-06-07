import request from '@/utils/request'
import type { AgentRequest, AgentResponse } from '@/types/agent'

export const sendAgentChat = (data: AgentRequest) => {
  return request.post<AgentResponse>('/agent/chat', data)
}

export const sendAgentChatWithMemory = (data: AgentRequest) => {
  return request.post<AgentResponse>('/agent/chat/memory', data)
}

export const sendAgentChatWithSpecificTools = (data: AgentRequest) => {
  return request.post<AgentResponse>('/agent/chat/specific', data)
}

export const agentReActStreamUrl = (
  message: string,
  conversationId?: string,
  mode = 'chat',
  tools: string[] = [],
  thinkingMode = true,
) => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const query = new URLSearchParams({ message, mode, thinkingMode: String(thinkingMode) })
  if (conversationId) query.set('conversationId', conversationId)
  if (tools.length) query.set('tools', tools.join(','))
  return `${baseUrl.replace(/\/$/, '')}/agent/chat/stream?${query.toString()}`
}

export const stopAgentReAct = (traceId: string) => {
  return request.post('/agent/chat/stop', { traceId })
}

export const listAgentReActTraces = (sessionId?: string) => {
  return request.get('/agent/chat/traces', { params: { sessionId } })
}

export const getAgentReActTrace = (traceId: string) => {
  return request.get(`/agent/chat/traces/${encodeURIComponent(traceId)}`)
}

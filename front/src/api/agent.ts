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

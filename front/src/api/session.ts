import request from '@/utils/request'
import type { ApiResponse } from '@/types/auth'
import type { SessionHistoryItem } from '@/types/session'

export const getSessionHistory = (conversationId: string) => {
  return request.get<ApiResponse<SessionHistoryItem[]>>(`/session/${encodeURIComponent(conversationId)}/history`)
}

export const clearSession = (conversationId: string) => {
  return request.delete<ApiResponse<string>>(`/session/${encodeURIComponent(conversationId)}`)
}

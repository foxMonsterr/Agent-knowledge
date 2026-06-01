import request from '@/utils/request'

export const getMonitorOverview = () => {
  return request.get('/monitor/overview')
}

export const getMonitorStats = () => {
  return request.get('/monitor/stats')
}

export const getMonitorHistory = (params: { username?: string; page?: number; size?: number }) => {
  return request.get('/monitor/history', { params })
}

export const getMonitorConversation = (conversationId: string) => {
  return request.get(`/monitor/conversation/${encodeURIComponent(conversationId)}`)
}

export const getMonitorSessions = (username: string) => {
  return request.get(`/monitor/sessions/${encodeURIComponent(username)}`)
}

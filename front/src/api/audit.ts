import request from '@/utils/request'

export interface AuditLogVO {
  traceId?: string
  conversationId?: string
  agentType?: string
  model?: string
  input?: string
  output?: string
  thinking?: string
  status?: string
  latencyMs?: number
  createdAt?: string
}

export const getAuditLogs = (conversationId?: string) =>
  request.get<AuditLogVO[]>('/audit/logs', { params: conversationId ? { conversationId } : {} })

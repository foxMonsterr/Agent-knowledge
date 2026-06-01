import request from '@/utils/request'

export interface OpsMetricVO {
  title: string
  value: string
  subTitle?: string
  trend?: string
  status?: string
}

export interface TrendPointVO {
  label: string
  value: number
}

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

export interface OpsDashboardVO {
  metrics: OpsMetricVO[]
  dailyChats: TrendPointVO[]
  dailyRuns: TrendPointVO[]
  dailyTokens: TrendPointVO[]
  recentAudits: AuditLogVO[]
}

export const getOpsMetrics = () => request.get('/ops/metrics')
export const getOpsDashboard = () => request.get('/ops/dashboard')

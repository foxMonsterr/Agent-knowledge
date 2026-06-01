import request from '@/utils/request'

export interface PerformanceSummaryVO {
  requestSuccessRate: number
  agentSuccessRate: number
  ragHitRate: number
  toolSuccessRate: number
  modelSuccessRate: number
  errorRate: number
  avgLatencyMs: number
}

export const getPerformanceSummary = () => request.get('/performance/summary')

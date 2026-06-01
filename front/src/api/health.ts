import request from '@/utils/request'
import type { ApiResponse } from '@/types/auth'

export interface HealthVO {
  status: string
  traceId?: string
  service?: string
}

export const getHealth = () => request.get<ApiResponse<HealthVO>>('/health')

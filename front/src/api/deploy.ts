import request from '@/utils/request'
import type { ApiResponse } from '@/types/auth'

export interface DeployInfoVO {
  appName: string
  profile: string
  javaVersion: string
  osName: string
  traceId?: string
  status: string
}

export interface DeployCheckVO {
  traceId?: string
  database: string
  redis: string
  health: string
  frontendProxy: string
}

export const getDeployInfo = () => request.get<ApiResponse<DeployInfoVO>>('/deploy/info')
export const getDeployCheck = () => request.get<ApiResponse<DeployCheckVO>>('/deploy/check')

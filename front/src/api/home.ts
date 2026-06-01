import request from '@/utils/request'
import type { ApiResponse } from '@/types/auth'
import type { OpsMetricVO } from './ops'

export interface HomeOverviewVO {
  title: string
  subtitle: string
  metrics: OpsMetricVO[]
  highlights: string[]
}

export const getHomeOverview = () => request.get<ApiResponse<HomeOverviewVO>>('/home/overview')

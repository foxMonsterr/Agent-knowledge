import type { ChatHistoryEntity } from './session'

export interface MonitorStats {
  totalUsers?: number
  totalChatRecords?: number
  totalAgentRuns?: number
  session?: Record<string, unknown>
  allTime?: Record<string, unknown>
}

export interface MonitorHistoryPage {
  content: ChatHistoryEntity[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

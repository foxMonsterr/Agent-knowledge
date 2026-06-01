import request from '@/utils/request'

export interface ReleaseChecklistVO {
  items: string[]
  status: string
}

export interface ReleaseSummaryVO {
  title: string
  highlights: string[]
  status: string
}

export const getReleaseChecklist = () => request.get('/release/checklist')
export const getReleaseSummary = () => request.get('/release/summary')

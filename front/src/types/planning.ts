export interface PlanningRequest {
  task: string
  conversationId?: string
  autoExecute?: boolean
  thinkingMode?: boolean
}

export interface StepResult {
  stepNumber: number
  description: string
  toolUsed?: string
  result?: string
  success: boolean
  timeMs?: number
}

export interface PlanningResponse {
  conversationId?: string
  taskSummary?: string
  planned: boolean
  directAnswer?: string
  steps?: StepResult[]
  finalAnswer?: string
  totalTimeMs?: number
  traceId?: string
}

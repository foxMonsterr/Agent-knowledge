import request from '@/utils/request'

export interface DemoFlowStepVO {
  title: string
  description: string
  route: string
  highlight: string
}

export const getDemoFlow = () => request.get<DemoFlowStepVO[]>('/demo/flow')

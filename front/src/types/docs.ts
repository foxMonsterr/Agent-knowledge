export interface ApiDocItem {
  module: string
  method: string
  path: string
  title: string
  description: string
  request?: string
  response?: string
  auth?: boolean
}

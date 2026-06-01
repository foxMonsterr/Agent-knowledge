import type { StreamMode } from '@/types/stream'

export const buildStreamUrl = (params: { message: string; conversationId?: string; mode: StreamMode }) => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const modePath = params.mode === 'tools' ? '/chat/tools' : '/chat'
  const query = new URLSearchParams({ message: params.message })
  if (params.conversationId) query.append('conversationId', params.conversationId)
  return `${baseUrl.replace(/\/$/, '')}/stream${modePath}?${query.toString()}`
}

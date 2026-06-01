import request from '@/utils/request'
import type { KnowledgeRequest, KnowledgeResponse } from '@/types/knowledge'

export const uploadDocument = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/knowledge/upload', formData)
}

export const loadDirectory = (path?: string) => {
  return request.post('/knowledge/load-directory', null, {
    params: { path },
  })
}

export const listDocuments = () => {
  return request.get('/knowledge/documents')
}

export const deleteDocument = (fileName: string) => {
  return request.delete(`/knowledge/documents/${encodeURIComponent(fileName)}`)
}

export const askKnowledge = (data: KnowledgeRequest) => {
  return request.post('/knowledge/ask', data) as Promise<KnowledgeResponse>
}

export const askKnowledgeManual = (data: KnowledgeRequest) => {
  return request.post('/knowledge/ask/manual', data) as Promise<KnowledgeResponse>
}

export const searchKnowledge = (query: string) => {
  return request.get('/knowledge/search', { params: { query } })
}

export const getKnowledgeStatus = () => {
  return request.get('/knowledge/status')
}

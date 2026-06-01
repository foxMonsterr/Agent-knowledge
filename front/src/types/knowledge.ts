export interface KnowledgeDocumentVO {
  fileName: string
  fileSize: number
  fileSizeReadable: string
  chunkCount: number
  indexTime: string
}

export interface KnowledgeRequest {
  question: string
  conversationId?: string
  topK?: number
  similarityThreshold?: number
}

export interface KnowledgeResponse {
  conversationId?: string
  answer?: string
  sources?: string[]
  retrievedChunks?: number
  model?: string
  traceId?: string
}

export interface KnowledgeStatusResponse {
  documentCount: number
  totalChunks: number
  documents: string[]
}

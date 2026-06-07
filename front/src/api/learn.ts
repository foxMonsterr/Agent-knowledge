import request from '@/utils/request'
import type { CreateNoteRequest, Flashcard, KnowledgeNote, QuizItem, ReActChatRequest } from '@/types/learn'

export const sendLearnChat = (data: ReActChatRequest) => {
  return request.post('/learn/chat', data)
}

export const learnChatStreamUrl = (message: string, sessionId?: string, strategy = 'auto') => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const query = new URLSearchParams({ message, strategy })
  if (sessionId) query.set('sessionId', sessionId)
  return `${baseUrl.replace(/\/$/, '')}/learn/chat/stream?${query.toString()}`
}

export const listLearnTraces = (sessionId?: string) => {
  return request.get('/learn/chat/traces', { params: { sessionId } })
}

export const getLearnTrace = (traceId: string) => {
  return request.get(`/learn/chat/traces/${encodeURIComponent(traceId)}`)
}

export const stopLearnChat = (traceId: string) => {
  return request.post('/learn/chat/stop', { traceId })
}

export const listNotes = () => {
  return request.get('/learn/notes') as Promise<KnowledgeNote[]>
}

export const createNote = (data: CreateNoteRequest) => {
  return request.post('/learn/notes', data) as Promise<KnowledgeNote>
}

export const importNoteDocument = (file: File, category?: string, tags?: string) => {
  const form = new FormData()
  form.append('file', file)
  if (category) form.append('category', category)
  if (tags) form.append('tags', tags)
  return request.post('/learn/notes/import', form) as Promise<KnowledgeNote>
}

export const searchNotes = (query: string) => {
  return request.post('/learn/notes/search', { query, topK: 8 })
}

export const generateQuiz = (noteId: string, count = 5) => {
  return request.post('/learn/quizzes/generate', { noteId, count }) as Promise<{ quizSetId: string; items: QuizItem[] }>
}

export const evaluateQuiz = (quizId: string, userAnswer: string) => {
  return request.post('/learn/quizzes/evaluate', { quizId, userAnswer })
}

export const generateFlashcards = (noteId: string, count = 10) => {
  return request.post('/learn/flashcards/generate', { noteId, count }) as Promise<{ items: Flashcard[] }>
}

export const listDueFlashcards = () => {
  return request.get('/learn/flashcards/due') as Promise<Flashcard[]>
}

export const reviewFlashcard = (cardId: string, quality: number) => {
  return request.post(`/learn/flashcards/${encodeURIComponent(cardId)}/review`, { quality })
}

export const getLearningOverview = () => {
  return request.get('/learn/progress/overview')
}

export const getWeakness = () => {
  return request.get('/learn/progress/weakness')
}

export const getKnowledgeGraph = () => {
  return request.get('/learn/graph')
}

export const getRadar = () => {
  return request.get('/learn/progress/radar')
}

export const getIntervention = () => {
  return request.get('/learn/progress/intervention')
}

export const getRecommendations = () => {
  return request.get('/learn/progress/recommendations')
}

export const getPriorityQueue = () => {
  return request.get('/learn/flashcards/priority-queue')
}

export const createPath = (data: { topic: string; targetNoteCount?: number; preferredDepth?: string }) => {
  return request.post('/learn/paths', data)
}

export const listPaths = () => {
  return request.get('/learn/paths')
}

export const getPathProgress = (pathId: string) => {
  return request.get(`/learn/paths/${encodeURIComponent(pathId)}`)
}

export const updatePathStage = (pathId: string, data: { stageId: string; status?: string; score?: number }) => {
  return request.put(`/learn/paths/${encodeURIComponent(pathId)}/stages`, data)
}

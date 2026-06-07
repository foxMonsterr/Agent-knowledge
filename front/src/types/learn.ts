import type { ReActStrategy } from './react'

export type { ReActSseEvent as LearnSseEvent, ReActSourceRef, ReActStep, ReActStrategy } from './react'

export interface ReActChatRequest {
  sessionId?: string
  message: string
  strategy?: ReActStrategy
  tags?: string[]
  category?: string
  autoCreateNote?: boolean
}

export interface KnowledgeNote {
  noteId: string
  title: string
  content: string
  summary: string
  tags: string[]
  category?: string
  sourceType: string
  masteryLevel: number
  reviewCount: number
  archived: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateNoteRequest {
  title: string
  content: string
  tags?: string[]
  category?: string
  sourceTraceId?: string
}

export interface QuizItem {
  quizId: string
  quizSetId: string
  noteId: string
  type: string
  question: string
  options?: { key: string; text: string }[]
  correctAnswer?: string
  explanation?: string
  difficulty: string
  tags: string[]
}

export interface Flashcard {
  cardId: string
  noteId: string
  front: string
  back: string
  tags: string[]
  easeFactor: number
  intervalDays: number
  reviewCount: number
  nextReviewAt: string
}

export interface GraphSummary {
  masteredCount: number
  learningCount: number
  weakCount: number
  newCount: number
  averageMastery: number
}

export interface GraphNode {
  id: string
  label: string
  colorGroup: string
  masteryLevel: number
  reviewCount: number
  category: string
}

export interface GraphEdge {
  source: string
  target: string
  relation: string
  weight: number
}

export interface RadarDimension {
  dimension: string
  noteCount: number
  averageMastery: number
  engagement: number
  weakNoteCount: number
  masteredNoteCount: number
}

export interface RadarData {
  userId: string
  totalNotes: number
  dimensions: RadarDimension[]
  overallMastery: number
}

export interface InterventionSuggestion {
  weaknessId: string
  topic: string
  severity: 'high' | 'medium'
  actions: SuggestedAction[]
  reason: string
}

export interface SuggestedAction {
  action: string
  label: string
  params: Record<string, string>
}

export interface InterventionData {
  userId: string
  weakCount: number
  suggestions: InterventionSuggestion[]
}

export interface RecommendationsData {
  userId: string
  insufficientData: boolean
  bestStudyHour: { hour: number; sessionCount: number } | null
  preferredMode: { activityType: string; count: number } | null
  recommendedTopics: { topic: string; totalSeconds: number }[]
  suggestedNotes: { noteId: string; title: string; masteryLevel: number; category: string }[]
  message?: string
}

export interface ReviewQueueItem {
  cardId: string
  noteId: string
  front: string
  back: string
  easeFactor: number
  intervalDays: number
  masteryLevel: number
  overdueDays: number
  priority: number
}

export interface PriorityQueueStats {
  userId: string
  totalDue: number
  highPriority: number
  mediumPriority: number
  lowPriority: number
  items: ReviewQueueItem[]
}

export interface LearningStage {
  stageId: string
  order: number
  title: string
  description: string
  action: string
  status: 'pending' | 'completed'
  score: number
  resourceNoteId?: string
  completedAt?: string
}

export interface LearningPathSummary {
  pathId: string
  topic: string
  status: string
  completedStages: number
  totalStages: number
  createdAt?: string
}

export interface LearningPathProgress extends LearningPathSummary {
  progress: number
  stages: LearningStage[]
}

export interface CreatePathRequest {
  topic: string
  targetNoteCount?: number
  preferredDepth?: string
}

export interface UpdateStageRequest {
  stageId: string
  status?: string
  score?: number
}

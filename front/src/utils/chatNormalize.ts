export const normalizeAnswer = (value: { answer?: string; reply?: string } | null | undefined, fallback = '-') => {
  if (!value) return fallback
  return value.answer || value.reply || fallback
}

export const normalizeSessionId = (value: { sessionId?: string; conversationId?: string } | null | undefined, fallback = '-') => {
  if (!value) return fallback
  return value.sessionId || value.conversationId || fallback
}

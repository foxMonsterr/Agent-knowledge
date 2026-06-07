import { computed, ref } from 'vue'
import { useUserStore } from '@/store/modules/user'
import type { ReActSseEvent, ReActSourceRef, ReActStep, ReActStrategy } from '@/types/react'

export interface UseReActStreamOptions {
  streamUrl: (message: string, sessionId?: string, strategy?: ReActStrategy, extra?: Record<string, unknown>) => string
  stop?: (traceId: string) => Promise<unknown>
}

export function useReActStream(options: UseReActStreamOptions) {
  const userStore = useUserStore()
  const status = ref<'idle' | 'connecting' | 'streaming' | 'completed' | 'error' | 'stopped'>('idle')
  const traceId = ref('')
  const sessionId = ref('')
  const answer = ref('')
  const error = ref('')
  const steps = ref<ReActStep[]>([])
  const sources = ref<ReActSourceRef[]>([])
  const abortController = ref<AbortController | null>(null)

  const isRunning = computed(() => status.value === 'connecting' || status.value === 'streaming')

  const stop = (notifyBackend = true) => {
    const currentTraceId = traceId.value
    abortController.value?.abort()
    abortController.value = null
    if (isRunning.value) status.value = 'stopped'
    if (notifyBackend && currentTraceId && options.stop) {
      void options.stop(currentTraceId).catch(() => undefined)
    }
  }

  const clear = () => {
    stop(false)
    status.value = 'idle'
    traceId.value = ''
    sessionId.value = ''
    answer.value = ''
    error.value = ''
    steps.value = []
    sources.value = []
  }

  const start = async (
    message: string,
    currentSessionId?: string,
    strategy: ReActStrategy = 'auto',
    extra?: Record<string, unknown>,
  ) => {
    clear()
    status.value = 'connecting'
    abortController.value = new AbortController()
    try {
      const res = await fetch(options.streamUrl(message, currentSessionId, strategy, extra), {
        signal: abortController.value.signal,
        headers: {
          Accept: 'text/event-stream',
          ...(userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {}),
        },
      })
      if (!res.ok) throw new Error(await readErrorMessage(res))
      if (!res.body) throw new Error('SSE 响应为空')

      status.value = 'streaming'
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split(/\n\n|\r\n\r\n/)
        buffer = parts.pop() || ''
        parts.forEach(handleEventBlock)
      }

      if (buffer.trim()) handleEventBlock(buffer)
      const currentStatus = status.value as string
      if (currentStatus !== 'stopped' && currentStatus !== 'error') status.value = 'completed'
    } catch (err: any) {
      const currentStatus = status.value as string
      if (currentStatus === 'stopped' || err?.name === 'AbortError') {
        status.value = 'stopped'
        return
      }
      status.value = 'error'
      error.value = err?.message || '流式请求失败'
      throw err
    }
  }

  const handleEventBlock = (part: string) => {
    const payloads = part
      .split(/\r?\n/)
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.replace(/^data:\s?/, ''))
    if (payloads.length) {
      payloads.forEach(handleRawEvent)
      return
    }
    const raw = part.trim()
    if (raw) handleRawEvent(raw)
  }

  const handleRawEvent = (raw: string) => {
    try {
      const event = JSON.parse(raw) as ReActSseEvent
      traceId.value = event.traceId || traceId.value
      sessionId.value = event.conversationId || event.sessionId || sessionId.value
      const stepNumber = event.stepNumber || event.data?.stepNumber || steps.value.length + 1
      if (event.type === 'thought') {
        steps.value.push({ stepNumber, type: 'thought', thought: event.data.content, state: event.data.state, status: event.data.status })
      } else if (event.type === 'action') {
        steps.value.push({ stepNumber, type: 'action', actionName: event.data.toolName, actionInput: event.data.input, state: event.data.state, status: event.data.status })
      } else if (event.type === 'observation') {
        steps.value.push({ stepNumber, type: 'observation', observation: event.data.content, durationMs: event.data.durationMs, state: event.data.state, status: event.data.status, errorMessage: event.data.errorMessage })
      } else if (event.type === 'source') {
        sources.value = event.data.sources || []
      } else if (event.type === 'final_answer') {
        answer.value = event.data.answer || event.data.finalContent || ''
        sources.value = event.data.sources || sources.value
      } else if (event.type === 'delta') {
        answer.value += event.data.content || ''
      } else if (event.type === 'error') {
        status.value = 'error'
        error.value = event.data.message || '流式请求失败'
      } else if (event.type === 'done') {
        status.value = event.data.status === 'stopped' ? 'stopped' : 'completed'
      }
    } catch {
      answer.value += raw
    }
  }

  const readErrorMessage = async (res: Response) => {
    try {
      const text = await res.text()
      if (!text) return `流式请求失败：HTTP ${res.status}`
      const body = JSON.parse(text)
      return body?.message || body?.data?.message || `流式请求失败：HTTP ${res.status}`
    } catch {
      return `流式请求失败：HTTP ${res.status}`
    }
  }

  return { status, traceId, sessionId, answer, error, steps, sources, isRunning, start, stop, clear }
}

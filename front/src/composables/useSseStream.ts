import { computed, onBeforeUnmount, ref } from 'vue'
import { useUserStore } from '@/store/modules/user'
import type { StreamChatParams, StreamEventPayload, StreamMessageLog, StreamStatus } from '@/types/stream'

const DEFAULT_ENDPOINTS: Record<string, string> = {
  basic: '/stream/chat',
  tools: '/stream/chat/tools',
  knowledge: '/knowledge/stream',
  planning: '/planning/stream',
  agent: '/stream/chat/tools',
}

function buildSseUrl(params: StreamChatParams) {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const endpoint = params.endpoint || DEFAULT_ENDPOINTS[params.mode || 'basic'] || DEFAULT_ENDPOINTS.basic
  const query = new URLSearchParams()
  query.set('message', params.message)
  if (params.conversationId) query.set('conversationId', params.conversationId)

  if (params.query) {
    Object.entries(params.query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
  }

  return `${baseUrl.replace(/\/$/, '')}${endpoint}?${query.toString()}`
}

function parseSseChunk(buffer: string) {
  const events = buffer.split(/\n\n|\r\n\r\n/)
  const remain = buffer.endsWith('\n\n') || buffer.endsWith('\r\n\r\n') ? '' : events.pop() || ''
  const payloads: string[] = []

  for (const event of events) {
    const lines = event.split(/\r?\n/)
    for (const line of lines) {
      if (line.startsWith('data:')) payloads.push(line.replace(/^data:\s?/, ''))
    }
  }

  return { payloads, remain }
}

function parseEventPayload(raw: string): StreamEventPayload {
  const text = raw.trim()
  if (!text) return { type: 'status', raw }
  if (text === '[DONE]') return { type: 'done', raw }

  try {
    const parsed = JSON.parse(text) as Partial<StreamEventPayload> & { event?: string; content?: string; message?: string; data?: string }
    const eventType = parsed.type || (parsed.event as StreamEventPayload['type']) || 'delta'
    const content = parsed.content || parsed.message || parsed.data || text
    return { type: eventType, content, raw }
  } catch {
    return { type: 'delta', content: text, raw }
  }
}

export function useSseStream() {
  const userStore = useUserStore()

  const status = ref<StreamStatus>('idle')
  const answer = ref('')
  const error = ref('')
  const conversationId = ref('')
  const logs = ref<StreamMessageLog[]>([])
  const abortController = ref<AbortController | null>(null)
  const startedAt = ref<number | null>(null)
  const endedAt = ref<number | null>(null)

  const isRunning = computed(() => ['connecting', 'streaming'].includes(status.value))
  const elapsedMs = computed(() => (startedAt.value ? (endedAt.value || Date.now()) - startedAt.value : 0))

  const addLog = (type: StreamMessageLog['type'], content: string) => {
    logs.value.unshift({
      id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      time: new Date().toLocaleString(),
      type,
      content,
    })
  }

  const emitEvent = (payload: StreamEventPayload) => {
    switch (payload.type) {
      case 'start':
        answer.value = ''
        addLog('status', payload.content || '流式开始')
        break
      case 'delta':
      case 'message':
        if (payload.content) {
          answer.value += payload.content
          addLog('assistant', payload.content)
        }
        break
      case 'done':
        addLog('status', payload.content || '流式输出完成')
        break
      case 'error':
        error.value = payload.content || '流式请求失败'
        addLog('error', error.value)
        break
      case 'status':
      default:
        if (payload.content) addLog('status', payload.content)
        break
    }
  }

  const stop = () => {
    if (abortController.value) {
      abortController.value.abort()
      abortController.value = null
    }
    if (status.value === 'streaming' || status.value === 'connecting') {
      status.value = 'stopped'
      endedAt.value = Date.now()
      addLog('status', '已主动停止流式连接')
    }
  }

  const clear = () => {
    stop()
    answer.value = ''
    error.value = ''
    logs.value = []
    status.value = 'idle'
    startedAt.value = null
    endedAt.value = null
  }

  const startFetchStream = async (params: StreamChatParams) => {
    stop()
    answer.value = ''
    error.value = ''
    startedAt.value = Date.now()
    endedAt.value = null
    status.value = 'connecting'

    if (params.conversationId) conversationId.value = params.conversationId

    const url = buildSseUrl(params)
    addLog('user', params.message)
    addLog('status', `开始流式请求: ${params.mode || 'basic'}`)

    abortController.value = new AbortController()

    try {
      const headers: Record<string, string> = { Accept: 'text/event-stream' }
      if (userStore.token) headers.Authorization = `Bearer ${userStore.token}`

      const res = await fetch(url, {
        method: 'GET',
        headers,
        signal: abortController.value.signal,
      })

      if (res.status === 401) {
        userStore.clearUser()
        error.value = '未授权，请重新登录'
        status.value = 'error'
        endedAt.value = Date.now()
        addLog('error', error.value)
        return
      }

      if (!res.ok || !res.body) throw new Error(`请求失败，HTTP ${res.status}`)

      status.value = 'streaming'
      addLog('status', '已建立 SSE 连接')

      const reader = res.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const parsed = parseSseChunk(buffer)
        buffer = parsed.remain

        for (const chunk of parsed.payloads) {
          const event = parseEventPayload(chunk)
          emitEvent(event)
        }
      }

      if (buffer.trim()) emitEvent(parseEventPayload(buffer))
      if (!answer.value && error.value) {
        status.value = 'error'
        endedAt.value = Date.now()
        addLog('error', error.value)
        return
      }
      status.value = 'completed'
      endedAt.value = Date.now()
      addLog('status', `流式输出完成，耗时 ${elapsedMs.value}ms`)
    } catch (err: any) {
      if (err?.name === 'AbortError') {
        status.value = 'stopped'
        endedAt.value = Date.now()
        addLog('status', '请求已中断')
        return
      }
      status.value = 'error'
      endedAt.value = Date.now()
      error.value = err?.message || '流式请求失败'
      addLog('error', error.value)
    } finally {
      abortController.value = null
    }
  }

  onBeforeUnmount(() => {
    stop()
  })

  return {
    status,
    answer,
    error,
    conversationId,
    logs,
    isRunning,
    elapsedMs,
    startFetchStream,
    stop,
    clear,
    emitEvent,
  }
}

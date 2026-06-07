import { computed, onBeforeUnmount, ref } from 'vue'
import { useUserStore } from '@/store/modules/user'
import type { ConversationSseEvent, ConversationStreamState } from '@/types/conversation'
import type { ReActSourceRef, ReActStep } from '@/types/react'
import { conversationStreamUrl, type ConversationStreamParams } from '@/api/conversation'

function parseSseBlocks(buffer: string) {
  const blocks = buffer.split(/\n\n|\r\n\r\n/)
  const remain = buffer.endsWith('\n\n') || buffer.endsWith('\r\n\r\n') ? '' : blocks.pop() || ''
  return { blocks, remain }
}

function extractPayloads(block: string) {
  const lines = block.split(/\r?\n/)
  const payloads = lines.filter((line) => line.startsWith('data:')).map((line) => line.replace(/^data:\s?/, ''))
  return payloads.length ? payloads : [block.trim()].filter(Boolean)
}

export function useConversationStream() {
  const userStore = useUserStore()
  const status = ref<'idle' | 'connecting' | 'streaming' | 'completed' | 'stopped' | 'error'>('idle')
  const conversationId = ref('')
  const traceId = ref('')
  const answer = ref('')
  const finalContent = ref('')
  const error = ref('')
  const events = ref<ConversationSseEvent[]>([])
  const steps = ref<ReActStep[]>([])
  const sources = ref<ReActSourceRef[]>([])
  const abortController = ref<AbortController | null>(null)

  const isRunning = computed(() => status.value === 'connecting' || status.value === 'streaming')

  const clear = () => {
    stop()
    status.value = 'idle'
    conversationId.value = ''
    traceId.value = ''
    answer.value = ''
    finalContent.value = ''
    error.value = ''
    events.value = []
    steps.value = []
    sources.value = []
  }

  const stop = () => {
    abortController.value?.abort()
    abortController.value = null
    if (isRunning.value) status.value = 'stopped'
  }

  const handleEvent = (event: ConversationSseEvent) => {
    events.value.push(event)
    conversationId.value = event.conversationId || conversationId.value
    traceId.value = event.traceId || traceId.value
    const data: any = event.data || {}

    if (event.type === 'delta') {
      answer.value += data.content || ''
    } else if (event.type === 'thought') {
      steps.value.push({ stepNumber: data.stepNumber || steps.value.length + 1, type: 'thought', thought: data.content, state: data.state, status: data.status })
    } else if (event.type === 'action') {
      steps.value.push({ stepNumber: data.stepNumber || steps.value.length + 1, type: 'action', actionName: data.toolName, actionInput: data.input, state: data.state, status: data.status })
    } else if (event.type === 'observation') {
      steps.value.push({ stepNumber: data.stepNumber || steps.value.length + 1, type: 'observation', observation: data.content, durationMs: data.durationMs, state: data.state, status: data.status, errorMessage: data.errorMessage })
    } else if (event.type === 'source') {
      sources.value = data.sources || []
    } else if (event.type === 'final_answer') {
      answer.value = data.answer || data.finalContent || answer.value
      sources.value = data.sources || sources.value
    } else if (event.type === 'error') {
      status.value = 'error'
      error.value = data.message || '流式请求失败'
    } else if (event.type === 'done') {
      finalContent.value = data.finalContent || answer.value
      status.value = data.status === 'stopped' ? 'stopped' : data.status === 'failed' ? 'error' : 'completed'
    }
  }

  const start = async (params: ConversationStreamParams) => {
    clear()
    status.value = 'connecting'
    abortController.value = new AbortController()
    try {
      const res = await fetch(conversationStreamUrl(params), {
        signal: abortController.value.signal,
        headers: {
          Accept: 'text/event-stream',
          ...(userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {}),
        },
      })
      if (!res.ok) throw new Error(`流式请求失败：HTTP ${res.status}`)
      if (!res.body) throw new Error('SSE 响应为空')

      status.value = 'streaming'
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const parsed = parseSseBlocks(buffer)
        buffer = parsed.remain
        parsed.blocks.flatMap(extractPayloads).forEach((raw) => handleEvent(JSON.parse(raw)))
      }
      if (buffer.trim()) extractPayloads(buffer).forEach((raw) => handleEvent(JSON.parse(raw)))
      if (status.value === 'streaming') status.value = 'completed'
    } catch (err: any) {
      if (err?.name === 'AbortError') {
        status.value = 'stopped'
        return
      }
      status.value = 'error'
      error.value = err?.message || '流式请求失败'
    } finally {
      abortController.value = null
    }
  }

  onBeforeUnmount(stop)

  const state = computed<ConversationStreamState>(() => ({ answer: answer.value, steps: steps.value, sources: sources.value }))

  return { status, conversationId, traceId, answer, finalContent, error, events, steps, sources, state, isRunning, start, stop, clear }
}

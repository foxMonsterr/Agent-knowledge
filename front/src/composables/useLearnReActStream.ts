import { computed } from 'vue'
import { stopLearnChat } from '@/api/learn'
import { useConversationStream } from '@/composables/useConversationStream'
import type { ReActStrategy } from '@/types/learn'

export function useLearnReActStream() {
  const stream = useConversationStream()

  const start = (message: string, conversationId?: string, strategy: ReActStrategy = 'auto', memoryEnabled = true) => {
    return stream.start({
      message,
      conversationId,
      agentType: 'learn-react',
      mode: 'learn',
      strategy,
      memoryEnabled,
    })
  }

  const stop = () => {
    if (stream.traceId.value) {
      void stopLearnChat(stream.traceId.value).catch(() => undefined)
    }
    stream.stop()
  }

  return {
    ...stream,
    sessionId: computed(() => stream.conversationId.value),
    start,
    stop,
  }
}

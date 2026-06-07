<template>
  <div class="page-wrap">
    <el-row :gutter="16" class="full-height">
      <el-col :span="5" class="side-col">
        <el-card shadow="never" class="panel-card left-panel">
          <template #header>
            <div class="header">
              <span>会话信息</span>
              <el-tag type="info">Agent</el-tag>
            </div>
          </template>

          <el-form :model="form" label-width="90px" class="compact-form">
            <el-form-item label="conversationId">
              <el-input v-model="form.conversationId" placeholder="可选" clearable />
            </el-form-item>
            <el-form-item label="模式">
              <el-select v-model="form.mode" placeholder="请选择模式" style="width: 100%">
                <el-option label="工具调用" value="chat" />
                <el-option label="工具调用+记忆" value="memory" />
                <el-option label="指定工具" value="specific" />
              </el-select>
            </el-form-item>
            <el-form-item label="ReAct">
              <el-switch
                v-model="form.thinkingMode"
                active-text="思考链路"
                inactive-text="普通"
              />
            </el-form-item>
            <el-form-item label="Memory">
              <el-switch
                v-model="form.memoryEnabled"
                active-text="开启"
                inactive-text="关闭"
              />
            </el-form-item>
            <el-form-item v-if="form.mode === 'specific'" label="tools">
              <el-select v-model="form.tools" multiple placeholder="请选择工具" style="width: 100%">
                <el-option label="datetime" value="datetime" />
                <el-option label="calculator" value="calculator" />
                <el-option label="translate" value="translate" />
                <el-option label="doc" value="doc" />
                <el-option label="db" value="db" />
                <el-option label="text" value="text" />
                <el-option label="json" value="json" />
                <el-option label="collection" value="collection" />
                <el-option label="regex" value="regex" />
                <el-option label="system" value="system" />
              </el-select>
            </el-form-item>
          </el-form>

          <div class="hint-box">
            <p>此页支持：会话记忆、工具调用、指定工具集。</p>
            <p>适合观察 Agent 的执行结果和日志。</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="14" class="main-col">
        <el-card shadow="never" class="panel-card chat-panel">
          <template #header>
            <div class="card-header">
              <div>
                <span>Agent 会话</span>
                <el-tag class="ml8" :type="statusTagType">{{ statusText }}</el-tag>
              </div>
              <div class="chat-header-meta">
                <el-tag type="info" effect="plain">Conversation: {{ form.conversationId || '-' }}</el-tag>
              </div>
            </div>
          </template>

          <div ref="chatBodyRef" class="chat-body">
            <div v-if="!messages.length && !liveAnswer" class="empty-state">
              <div class="empty-title">开始调用 Agent</div>
              <div class="empty-subtitle">支持记忆、指定工具、统一消息流展示，并已接入流式输出</div>
            </div>

            <div v-for="item in messages" :key="item.id" class="message-item" :class="item.role">
              <div class="message-avatar">{{ item.role === 'user' ? '你' : item.role === 'assistant' ? 'AI' : 'i' }}</div>
              <div class="message-content">
                <div class="message-meta">{{ item.meta }}</div>
                <div class="message-text">{{ item.content }}</div>
              </div>
            </div>

            <div v-if="streaming" class="message-item assistant live">
              <div class="message-avatar">AI</div>
              <div class="message-content">
                <div class="message-meta">流式生成中</div>
                <div class="message-text">{{ displayLiveAnswer }}</div>
              </div>
            </div>
          </div>

          <div class="composer">
            <el-input
              v-model="form.message"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="输入消息，Enter 发送，Shift+Enter 换行"
              @keydown.enter.exact.prevent="handleSend"
            />
            <div class="composer-actions">
              <el-button type="primary" :loading="loading" @click="handleSend">发送</el-button>
              <el-button :disabled="!streaming" @click="handleStopStream">停止生成</el-button>
              <el-button @click="handleReset">清空</el-button>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card mt16">
          <template #header>
            <div class="header">
              <span>响应结果</span>
              <el-tag type="success">结果面板</el-tag>
            </div>
          </template>
          <div class="response-box">
            <template v-if="response">
              <p><b>sessionId:</b> {{ normalizedSessionId(response) }}</p>
              <p><b>reply:</b></p>
              <pre>{{ normalizedReply(response) }}</pre>
              <p><b>traceId:</b> {{ response.traceId || '-' }}</p>
              <p><b>code:</b> {{ response.code ?? '-' }}</p>
              <p><b>message:</b> {{ response.message || '-' }}</p>
              <p><b>model:</b> {{ response.model || '-' }}</p>
              <p><b>agentType:</b> {{ response.agentType || '-' }}</p>
              <template v-if="response.thinking">
                <p><b>thinking:</b></p>
                <pre>{{ response.thinking }}</pre>
              </template>
            </template>
            <el-empty v-else description="暂无数据" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="5" class="side-col">
        <ReActChainPanel
          v-if="form.thinkingMode"
          class="right-panel"
          title="Agent ReAct 链路"
          :status="reactStatus"
          :steps="reactSteps"
          empty-text="开启 ReAct 后，这里会展示通用 Agent 的工具调用链路。"
        />

        <el-card v-else shadow="never" class="panel-card right-panel">
          <template #header>
            <div class="header">
              <span>请求日志</span>
              <el-button text size="small" @click="logs = []">清空</el-button>
            </div>
          </template>
          <div class="log-list">
            <div v-for="item in logs" :key="item.id" class="log-item" :class="item.type">
              <div class="log-meta">
                <span>{{ item.time }}</span>
                <el-tag size="small" :type="tagType(item.type)">{{ item.type }}</el-tag>
              </div>
              <div class="log-content">{{ item.content }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { AgentMode, AgentResponse } from '@/types/agent'
import { normalizeAnswer, normalizeSessionId } from '@/utils/chatNormalize'
import { stopAgentReAct } from '@/api/agent'
import ReActChainPanel from '@/components/react/ReActChainPanel.vue'
import { useConversationStream } from '@/composables/useConversationStream'

const loading = ref(false)
const response = ref<AgentResponse | null>(null)
const logs = ref<Array<{ id: string; type: 'request' | 'success' | 'error' | 'info'; time: string; content: string }>>([])
const messages = ref<Array<{ id: string; role: 'user' | 'assistant' | 'system'; content: string; meta: string }>>([])
const chatBodyRef = ref<HTMLElement | null>(null)

const form = reactive({
  conversationId: '',
  message: '',
  mode: 'chat' as AgentMode,
  tools: [] as string[],
  thinkingMode: false,
  memoryEnabled: true,
})

const {
  answer: liveAnswer,
  status: streamStatus,
  traceId,
  conversationId,
  steps: reactSteps,
  isRunning: streaming,
  clear: clearStream,
  start: startConversationStream,
  stop: stopConversationStream,
} = useConversationStream()

const reactStatus = computed(() => streamStatus.value)
const displayLiveAnswer = computed(() => liveAnswer.value)

const addLog = (type: 'request' | 'success' | 'error' | 'info', content: string) => {
  logs.value.unshift({ id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`, type, time: new Date().toLocaleString(), content })
}

const scrollToBottom = () => {
  requestAnimationFrame(() => {
    const el = chatBodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

const appendMessage = (role: 'user' | 'assistant' | 'system', content: string, meta = '') => {
  messages.value.push({
    id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    role,
    content,
    meta,
  })
  scrollToBottom()
}

const tagType = (type: string) => {
  if (type === 'error') return 'danger'
  if (type === 'success') return 'success'
  if (type === 'request') return 'warning'
  return 'info'
}

const statusText = computed(() => {
  if (loading.value) return '处理中'
  if (streaming.value) return form.thinkingMode ? 'ReAct 流式中' : '流式中'
  return '空闲'
})
const statusTagType = computed(() => {
  if (loading.value || streaming.value) return 'warning'
  return 'success'
})
const normalizedReply = (value: AgentResponse | null) => normalizeAnswer(value || undefined, '-')
const normalizedSessionId = normalizeSessionId

const handleSend = async () => {
  const message = form.message.trim()
  if (!message) {
    ElMessage.warning('请输入消息内容')
    return
  }

  loading.value = true
  clearStream()
  form.message = ''
  appendMessage('user', message, 'Agent 请求')
  response.value = null
  try {
    addLog('request', `agentType=${form.thinkingMode ? 'general-react' : 'tool'}, mode=${form.thinkingMode ? 'react' : form.mode}, memory=${form.memoryEnabled}, tools=${form.tools.join(',') || '-'}`)
    await startConversationStream({
      message,
      conversationId: form.conversationId.trim() || undefined,
      agentType: form.thinkingMode ? 'general-react' : 'tool',
      mode: form.thinkingMode ? 'react' : form.mode,
      thinkingMode: form.thinkingMode,
      tools: [...form.tools],
      memoryEnabled: form.memoryEnabled,
    })

    const finalAnswer = liveAnswer.value
    if (conversationId.value) form.conversationId = conversationId.value
    liveAnswer.value = ''
    response.value = {
      conversationId: conversationId.value || form.conversationId.trim() || undefined,
      reply: finalAnswer,
      model: form.thinkingMode ? 'react-stream' : 'agent-stream',
      agentType: form.thinkingMode ? 'general-react' : 'tool',
      traceId: traceId.value,
      thinking: form.thinkingMode ? reactSteps.value.map((step) => step.thought || step.actionName || step.observation || step.type).join('\n') : undefined,
    } as AgentResponse
    appendMessage('assistant', normalizedReply(response.value), `model: ${response.value?.model || '-'}`)
    addLog('success', '请求成功')
  } catch (e: any) {
    appendMessage('system', e?.message || '请求失败')
    addLog('error', e?.message || '请求失败')
  } finally {
    loading.value = false
  }
}

const handleStopStream = () => {
  if (form.thinkingMode) {
    if (traceId.value) void stopAgentReAct(traceId.value).catch(() => undefined)
    stopConversationStream()
    addLog('info', '停止 ReAct 流式生成')
  } else {
    stopConversationStream()
    addLog('info', '停止流式生成')
  }
}

const handleReset = () => {
  form.conversationId = ''
  form.message = ''
  form.mode = 'chat'
  form.tools = []
  form.thinkingMode = false
  form.memoryEnabled = true
  response.value = null
  messages.value = []
  clearStream()
  addLog('info', '已清空 Agent 会话')
}
</script>

<style scoped lang="scss">
.page-wrap { height: 100%; }
.full-height { height: 100%; }
.side-col, .main-col { height: 100%; }
.panel-card { height: 100%; border-radius: 12px; }
.left-panel, .right-panel, .chat-panel { height: calc(100vh - 170px); display: flex; flex-direction: column; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.ml8 { margin-left: 8px; }
.compact-form :deep(.el-form-item) { margin-bottom: 12px; }
.hint-box { padding: 12px; background: #fafcff; border: 1px solid #ebeef5; border-radius: 8px; color: #606266; font-size: 13px; line-height: 1.7; }
.chat-body { flex: 1; min-height: 0; overflow: auto; padding: 8px 4px; background: #fafcff; border: 1px solid #ebeef5; border-radius: 10px; }
.empty-state { padding: 40px 16px; text-align: center; color: #909399; }
.empty-title { font-size: 18px; font-weight: 600; margin-bottom: 8px; color: #303133; }
.empty-subtitle { font-size: 13px; }
.message-item { display: flex; gap: 12px; margin: 12px 0; }
.message-item.user { flex-direction: row-reverse; }
.message-item.live .message-content { border-style: dashed; border-color: #f0b878; }
.message-avatar { width: 36px; height: 36px; border-radius: 50%; background: #e8f3ff; color: #409eff; display: flex; align-items: center; justify-content: center; font-weight: 700; flex-shrink: 0; }
.message-item.user .message-avatar { background: #fdf6ec; color: #e6a23c; }
.message-content { max-width: calc(100% - 60px); padding: 12px 14px; border-radius: 12px; background: #fff; border: 1px solid #ebeef5; }
.message-item.user .message-content { background: #ecf5ff; }
.message-meta { font-size: 12px; color: #909399; margin-bottom: 6px; }
.message-text { white-space: pre-wrap; word-break: break-word; line-height: 1.7; }
.composer { margin-top: 12px; display: flex; flex-direction: column; gap: 12px; }
.composer-actions { display: flex; justify-content: flex-end; gap: 10px; }
.response-box { min-height: 220px; }
.response-box pre { white-space: pre-wrap; word-break: break-word; background: #fafafa; padding: 12px; border-radius: 8px; }
.log-list { overflow: auto; min-height: 0; flex: 1; }
.log-item { padding: 12px; border: 1px solid #ebeef5; border-radius: 8px; margin-bottom: 12px; background: #fff; }
.log-item.request { border-left: 4px solid #e6a23c; }
.log-item.success { border-left: 4px solid #67c23a; }
.log-item.error { border-left: 4px solid #f56c6c; }
.log-item.info { border-left: 4px solid #909399; }
.log-meta { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: #909399; margin-bottom: 8px; }
.log-content { white-space: pre-wrap; word-break: break-word; line-height: 1.6; }
.mt16 { margin-top: 16px; }
</style>

<template>
  <div class="page-wrap">
    <el-row :gutter="16" class="full-height">
      <el-col :span="5" class="side-col">
        <el-card shadow="never" class="panel-card left-panel">
          <template #header>
            <div class="header">
              <span>会话信息</span>
              <el-tag type="info">规划 / Agent</el-tag>
            </div>
          </template>

          <el-form :model="form" label-width="90px" class="compact-form">
            <el-form-item label="conversationId">
              <el-input v-model="form.conversationId" placeholder="可选" clearable />
            </el-form-item>
            <el-form-item label="autoExecute">
              <el-switch v-model="form.autoExecute" active-text="自动" inactive-text="仅规划" />
            </el-form-item>
            <el-form-item label="Memory">
              <el-switch v-model="form.memoryEnabled" active-text="开启" inactive-text="关闭" />
            </el-form-item>
          </el-form>

          <div class="hint-box">
            <p>此页支持：会话记忆、流式输出、规划步骤展示。</p>
            <p>复杂任务建议使用“规划并执行”。</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="14" class="main-col">
        <el-card shadow="never" class="panel-card chat-panel">
          <template #header>
            <div class="card-header">
              <div>
                <span>任务规划 / 全能 Agent</span>
                <el-tag class="ml8" :type="statusTagType">{{ statusText }}</el-tag>
              </div>
              <div class="chat-header-meta">
                <el-tag type="info" effect="plain">Session: {{ form.conversationId || '-' }}</el-tag>
              </div>
            </div>
          </template>

          <div ref="chatBodyRef" class="chat-body">
            <div v-if="!messages.length && !liveAnswer" class="empty-state">
              <div class="empty-title">开始规划或调用 Agent</div>
              <div class="empty-subtitle">支持会话记忆、流式输出、步骤展示与结果回显</div>
            </div>

            <div v-for="item in messages" :key="item.id" class="message-item" :class="item.role">
              <div class="message-avatar">{{ item.role === 'user' ? '你' : item.role === 'assistant' ? 'AI' : 'i' }}</div>
              <div class="message-content">
                <div class="message-meta">{{ item.meta }}</div>
                <div class="message-text">{{ item.content }}</div>
              </div>
            </div>

            <div v-if="(loading.execute || loading.agent) && liveAnswer" class="message-item assistant live">
              <div class="message-avatar">AI</div>
              <div class="message-content">
                <div class="message-meta">流式生成中</div>
                <div class="message-text">{{ liveAnswer }}</div>
              </div>
            </div>
          </div>

          <div class="composer">
            <el-input
              v-model="form.task"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="输入复杂任务，Enter 发送，Shift+Enter 换行"
              @keydown.enter.exact.prevent="handlePlanAndExecute"
            />
            <div class="composer-actions">
              <el-button type="primary" :loading="loading.execute" @click="handlePlanAndExecute">规划并执行</el-button>
              <el-button :loading="loading.execute" @click="handlePlanOnly">仅规划</el-button>
              <el-button type="success" plain :loading="loading.agent" @click="handleAgentChat">Agent 对话</el-button>
              <el-button @click="handleStopStream">停止生成</el-button>
              <el-button @click="handleReset">清空</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="5" class="side-col">
        <el-card shadow="never" class="panel-card right-panel">
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
import { useConversationStream } from '@/composables/useConversationStream'

const loading = reactive({ execute: false, agent: false })
const logs = ref<Array<{ id: string; type: 'request' | 'success' | 'error' | 'info'; time: string; content: string }>>([])
const messages = ref<Array<{ id: string; role: 'user' | 'assistant' | 'system'; content: string; meta: string }>>([])
const chatBodyRef = ref<HTMLElement | null>(null)
const { answer: liveAnswer, finalContent, conversationId: streamConversationId, clear: clearStream, start: startConversationStream, stop: stopStream, status: streamStatus } = useConversationStream()

const form = reactive({
  conversationId: '',
  task: '',
  autoExecute: true,
  memoryEnabled: true,
})

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

const statusText = computed(() => (loading.execute || loading.agent || streamStatus.value === 'streaming' || streamStatus.value === 'connecting' ? '处理中' : '空闲'))
const statusTagType = computed(() => (loading.execute || loading.agent || streamStatus.value === 'streaming' || streamStatus.value === 'connecting' ? 'warning' : 'success'))

const validateTask = () => {
  if (!form.task.trim()) {
    ElMessage.warning('请输入任务内容')
    return false
  }
  return true
}

const handlePlanAndExecute = async () => {
  if (!validateTask()) return
  const task = form.task.trim()
  loading.execute = true
  clearStream()
  form.task = ''
  appendMessage('user', task, '规划并执行')
  addLog('request', '执行任务规划并执行')
  try {
    await startConversationStream({
      message: task,
      conversationId: form.conversationId.trim() || undefined,
      agentType: 'planning',
      mode: 'planning',
      autoExecute: form.autoExecute,
      memoryEnabled: form.memoryEnabled,
    })
    const finalAnswer = finalContent.value || liveAnswer.value
    if (streamConversationId.value) form.conversationId = streamConversationId.value
    liveAnswer.value = ''
    appendMessage('assistant', finalAnswer || '（空响应）', '规划结果')
    addLog('success', '执行成功')
  } catch (e: any) {
    appendMessage('system', e?.message || '执行失败')
    addLog('error', e?.message || '执行失败')
  } finally {
    loading.execute = false
  }
}

const handlePlanOnly = async () => {
  if (!validateTask()) return
  const task = form.task.trim()
  loading.execute = true
  clearStream()
  form.task = ''
  appendMessage('user', task, '仅规划')
  addLog('request', '仅执行规划')
  try {
    await startConversationStream({
      message: task,
      conversationId: form.conversationId.trim() || undefined,
      agentType: 'planning',
      mode: 'planning_only',
      autoExecute: false,
      memoryEnabled: form.memoryEnabled,
    })
    const finalAnswer = finalContent.value || liveAnswer.value
    if (streamConversationId.value) form.conversationId = streamConversationId.value
    liveAnswer.value = ''
    appendMessage('assistant', finalAnswer || '（空响应）', '规划结果')
    addLog('success', '规划成功')
  } catch (e: any) {
    appendMessage('system', e?.message || '规划失败')
    addLog('error', e?.message || '规划失败')
  } finally {
    loading.execute = false
  }
}

const handleAgentChat = async () => {
  if (!validateTask()) return
  const task = form.task.trim()
  loading.agent = true
  clearStream()
  form.task = ''
  appendMessage('user', task, 'Agent 对话')
  addLog('request', '调用全能 Agent 入口')
  try {
    await startConversationStream({
      message: task,
      conversationId: form.conversationId.trim() || undefined,
      agentType: 'full',
      mode: 'chat',
      memoryEnabled: form.memoryEnabled,
    })
    const finalAnswer = finalContent.value || liveAnswer.value
    if (streamConversationId.value) form.conversationId = streamConversationId.value
    liveAnswer.value = ''
    appendMessage('assistant', finalAnswer || '（空响应）', 'Agent 回复')
    addLog('success', 'Agent 调用成功')
  } catch (e: any) {
    appendMessage('system', e?.message || 'Agent 调用失败')
    addLog('error', e?.message || 'Agent 调用失败')
  } finally {
    loading.agent = false
  }
}

const handleStopStream = () => {
  stopStream()
  addLog('info', '停止流式生成')
}

const handleReset = () => {
  form.conversationId = ''
  form.task = ''
  form.autoExecute = true
  form.memoryEnabled = true
  messages.value = []
  clearStream()
  addLog('info', '已清空规划会话')
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

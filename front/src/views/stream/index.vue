<template>
  <div class="chat-shell">
    <aside class="sidebar">
      <div class="sidebar-header">
        <div>
          <div class="sidebar-title">会话</div>
          <div class="sidebar-subtitle">线程式对话</div>
        </div>
        <el-button type="primary" size="small" @click="handleNewSession">新建</el-button>
      </div>

      <div class="session-toolbar">
        <el-input v-model="sessionFilter" placeholder="搜索会话" clearable />
      </div>

      <div class="session-list">
        <div
          v-for="session in filteredSessions"
          :key="session.sessionId"
          class="session-card"
          :class="{ active: session.sessionId === currentSessionId }"
          @click="switchSession(session.sessionId)"
        >
          <div class="session-card-top">
            <div class="session-name">{{ session.title || '未命名会话' }}</div>
            <el-dropdown trigger="click" @command="(command: string) => handleSessionAction(command, session.sessionId)">
              <el-button text class="session-action-btn" @click.stop>
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="rename">重命名</el-dropdown-item>
                  <el-dropdown-item command="export-txt">导出 TXT</el-dropdown-item>
                  <el-dropdown-item command="export-html">导出 HTML</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="session-card-sub">{{ session.summary || '暂无摘要' }}</div>
          <div class="session-card-meta">
            <span>{{ session.lastMessageAt || '刚刚' }}</span>
            <el-tag size="small" :type="session.sessionId === currentSessionId ? 'success' : 'info'">{{ session.sessionId.slice(-6) }}</el-tag>
          </div>
        </div>
        <el-empty v-if="!filteredSessions.length" description="暂无会话" />
      </div>
    </aside>

    <main class="workspace">
      <header class="workspace-header">
        <div>
          <div class="workspace-title">基础对话</div>
          <div class="workspace-subtitle">记忆 + 流式</div>
        </div>
        <div class="workspace-meta">
          <el-tag :type="statusTagType">{{ statusText }}</el-tag>
          <el-tag type="info" effect="plain">{{ currentSessionId || '-' }}</el-tag>
        </div>
      </header>

      <section ref="chatBodyRef" class="message-panel">
        <div v-if="!messages.length && !liveAnswer" class="empty-state">
          <div class="empty-title">开始新的线程对话</div>
          <div class="empty-subtitle">每个会话独立保存上下文，不会串线</div>
        </div>

        <div v-for="item in messages" :key="item.id" class="message-item" :class="item.role">
          <div class="message-avatar">{{ item.role === 'user' ? 'user' : item.role === 'assistant' ? 'system' : 'system' }}</div>
          <div class="message-content">
            <div class="message-meta">{{ item.role }} · {{ item.meta }}</div>
            <div class="message-text">{{ item.content }}</div>
          </div>
        </div>

        <div v-if="liveAnswer" class="message-item assistant live">
          <div class="message-avatar">system</div>
          <div class="message-content">
            <div class="message-meta">system · 流式生成中</div>
            <div class="message-text">{{ liveAnswer }}</div>
          </div>
        </div>
      </section>

      <section class="composer">
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
          <el-button type="primary" :loading="streamState === 'streaming' || streamState === 'connecting'" @click="handleSend">发送</el-button>
          <el-button :disabled="!currentSessionId" @click="handleStop">停止</el-button>
          <el-button @click="handleClearMessages">清空当前线程</el-button>
        </div>
      </section>
    </main>

    <aside class="logs-drawer" :class="{ collapsed: logsCollapsed }">
      <div class="logs-drawer-header">
        <div class="logs-title-wrap">
          <div class="logs-title">请求日志</div>
          <div class="logs-subtitle">调试与流式状态</div>
        </div>
        <div class="logs-actions">
          <el-button text size="small" @click="logs = []">清空</el-button>
          <el-button text size="small" @click="logsCollapsed = !logsCollapsed">{{ logsCollapsed ? '展开' : '收起' }}</el-button>
        </div>
      </div>

      <div v-show="!logsCollapsed" class="log-list">
        <div v-for="item in logs" :key="item.id" class="log-item" :class="item.type">
          <div class="log-meta">
            <span>{{ item.time }}</span>
            <el-tag size="small" :type="logTagType(item.type)">{{ item.type }}</el-tag>
          </div>
          <div class="log-content">{{ item.content }}</div>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'
import { exportSession, getSessionMessages, createSession, listSessions, renameSession } from '@/api/chat'
import { clearSession } from '@/api/session'
import type { ChatMessageVO, SessionExportMessage, SessionVO } from '@/types/chat'
import { useSseStream } from '@/composables/useSseStream'

const { status: streamStatus, stop, clear, answer: liveAnswer, startFetchStream } = useSseStream()

const sessionFilter = ref('')
const sessions = ref<SessionVO[]>([])
const messages = ref<Array<{ id: string; role: 'user' | 'assistant' | 'system'; content: string; meta: string }>>([])
const currentSessionId = ref('')
const logs = ref<Array<{ id: string; type: 'request' | 'success' | 'error' | 'info'; time: string; content: string }>>([])
const logsCollapsed = ref(false)
const chatBodyRef = ref<HTMLElement | null>(null)
const form = reactive({ message: '' })

const streamState = computed(() => streamStatus.value)
const statusText = computed(() => (streamStatus.value === 'streaming' || streamStatus.value === 'connecting' ? '输出中' : '空闲'))
const statusTagType = computed(() => (streamStatus.value === 'streaming' || streamStatus.value === 'connecting' ? 'warning' : 'success'))

const logTagType = (type: string) => {
  if (type === 'error') return 'danger'
  if (type === 'success') return 'success'
  if (type === 'request') return 'warning'
  return 'info'
}

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
  messages.value.push({ id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`, role, content, meta })
  scrollToBottom()
}

// 流式生成时跟随滚动
watch(liveAnswer, () => {
  if (streamStatus.value === 'streaming') {
    scrollToBottom()
  }
})

const normalizeHistory = (list: ChatMessageVO[]) => {
  const output: Array<{ id: string; role: 'user' | 'assistant' | 'system'; content: string; meta: string }> = []
  list.forEach((item, index) => {
    if (item.role === 'user' || item.role === 'assistant' || item.role === 'system') {
      output.push({ id: `${currentSessionId.value}_${index}`, role: item.role, content: item.content, meta: item.createdAt || '' })
    }
  })
  return output
}

const loadSessions = async () => {
  const list = await listSessions()
  sessions.value = (list || []) as SessionVO[]
  if (!currentSessionId.value && sessions.value.length) await switchSession(sessions.value[0].sessionId)
}

const refreshSessionsAfterMutation = async (fallbackSessionId?: string) => {
  await loadSessions()
  if (fallbackSessionId && sessions.value.some(item => item.sessionId === fallbackSessionId)) {
    currentSessionId.value = fallbackSessionId
    await loadMessages(fallbackSessionId)
    return
  }
  if (!sessions.value.length) {
    currentSessionId.value = ''
    messages.value = []
    clear()
    return
  }
  if (!sessions.value.some(item => item.sessionId === currentSessionId.value)) {
    await switchSession(sessions.value[0].sessionId)
  }
}

const loadMessages = async (sessionId: string) => {
  const res = await getSessionMessages(sessionId)
  messages.value = normalizeHistory((res || []) as ChatMessageVO[])
  scrollToBottom()
}

const switchSession = async (sessionId: string) => {
  currentSessionId.value = sessionId
  clear()
  messages.value = []
  addLog('info', `切换会话: ${sessionId}`)
  await loadMessages(sessionId)
}

const handleNewSession = async () => {
  const data = await createSession({ title: '基础对话' })
  const sessionId = data?.sessionId || ''
  if (!sessionId) {
    ElMessage.error('创建会话失败')
    return
  }
  sessions.value.unshift({ ...data, title: data.title || '基础对话', summary: '新会话' })
  currentSessionId.value = sessionId
  messages.value = []
  form.message = ''
  clear()
  addLog('success', `新建会话: ${sessionId}`)
}

const handleSend = async () => {
  if (!form.message.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }
  if (!currentSessionId.value) {
    await handleNewSession()
    if (!currentSessionId.value) return
  }

  const content = form.message.trim()
  appendMessage('user', content, '你')
  form.message = ''
  addLog('request', `基础对话流式请求会话=${currentSessionId.value}`)

  try {
    await startFetchStream({
      message: content,
      conversationId: currentSessionId.value,
      mode: 'basic',
      endpoint: '/stream/chat',
    })
    addLog('success', '收到流式响应')
    await loadMessages(currentSessionId.value)
    liveAnswer.value = ''
    await loadSessions()
  } catch (e: any) {
    appendMessage('system', e?.message || '请求失败')
    addLog('error', e?.message || '请求失败')
  }
}

const handleStop = () => {
  stop()
  addLog('info', '停止流式输出')
}

const handleClearMessages = () => {
  clear()
  liveAnswer.value = ''
  messages.value = []
  addLog('info', '清空当前线程')
}

const buildExportText = (data: { messages?: SessionExportMessage[] }) => {
  return (data.messages || [])
    .map((item) => [
      item.role === 'user' ? 'user' : item.role === 'system' ? 'system' : 'system',
      (item.content || '').trim(),
    ])
    .filter(([, content]) => content.length > 0)
    .map(([role, content]) => `${role}\n${content}`)
    .join('\n\n')
}

const buildExportHtml = (data: { title?: string; messages?: SessionExportMessage[] }) => {
  const blocks = (data.messages || [])
    .map((item) => {
      const role = item.role === 'user' ? 'user' : item.role === 'system' ? 'system' : 'system'
      const content = (item.content || '').trim()
      if (!content) return ''
      const escaped = content
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
      return `<section class="msg msg-${role}"><div class="msg-role">${role}</div><pre class="msg-content">${escaped}</pre></section>`
    })
    .filter(Boolean)
    .join('\n')

  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8" /><meta name="viewport" content="width=device-width, initial-scale=1" /><title>${(data.title || '会话').replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')}</title><style>body{font-family:Arial,Helvetica,sans-serif;background:#f6f8fb;color:#1f2329;margin:0;padding:24px}.wrap{max-width:920px;margin:0 auto;background:#fff;border:1px solid #e5e9f2;border-radius:16px;padding:24px}.msg{padding:14px 16px;border:1px solid #e5e9f2;border-radius:12px;margin:14px 0}.msg-user{background:#fdf6ec;border-left:4px solid #e6a23c}.msg-system{background:#f4f4f5;border-left:4px solid #909399}.msg-role{font-size:12px;font-weight:700;color:#8a919f;margin-bottom:8px;text-transform:uppercase}.msg-content{white-space:pre-wrap;word-break:break-word;margin:0;font-family:inherit;line-height:1.8}</style></head><body><main class="wrap">${blocks || '<p>暂无内容</p>'}</main></body></html>`
}

const triggerDownload = (filename: string, content: string, mimeType: string) => {
  const blob = new Blob([content], { type: `${mimeType};charset=utf-8` })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

const handleSessionAction = async (command: string, sessionId: string) => {
  if (command === 'rename') {
    const session = sessions.value.find(item => item.sessionId === sessionId)
    const { value } = await ElMessageBox.prompt('请输入新的会话标题', '重命名会话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: session?.title || '未命名会话',
      inputPlaceholder: '请输入会话标题',
      inputValidator: value => (!!value && value.trim().length > 0) || '标题不能为空',
    })
    const title = value.trim()
    const updated = await renameSession(sessionId, title)
    sessions.value = sessions.value.map(item => item.sessionId === sessionId ? { ...item, ...updated, title: updated?.title || title } : item)
    if (currentSessionId.value === sessionId) {
      await loadMessages(sessionId)
    }
    ElMessage.success('会话已重命名')
    addLog('success', `会话重命名: ${sessionId}`)
    return
  }

  if (command === 'export-txt' || command === 'export-html') {
    const data = await exportSession(sessionId)
    const fileBaseName = `${(data?.title || 'session').replace(/[\\/:*?"<>|]/g, '_')}-${sessionId}`

    if (command === 'export-txt') {
      const plainText = buildExportText(data || {})
      triggerDownload(`${fileBaseName}.txt`, plainText, 'text/plain')
    } else {
      const html = buildExportHtml(data || {})
      triggerDownload(`${fileBaseName}.html`, html, 'text/html')
    }

    ElMessage.success('导出成功')
    addLog('success', `会话导出: ${sessionId}`)
    return
  }

  if (command === 'delete') {
    await ElMessageBox.confirm('确认删除该会话？删除后该会话的历史将从列表中移除。', '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await requestDeleteSession(sessionId)
  }
}

const requestDeleteSession = async (sessionId: string) => {
  await clearSession(sessionId)
  if (currentSessionId.value === sessionId) {
    currentSessionId.value = ''
    messages.value = []
    liveAnswer.value = ''
  }
  ElMessage.success('会话已删除')
  addLog('success', `会话删除: ${sessionId}`)
  await refreshSessionsAfterMutation()
}

const filteredSessions = computed(() => {
  const keyword = sessionFilter.value.trim().toLowerCase()
  if (!keyword) return sessions.value
  return sessions.value.filter(item => `${item.title || ''} ${item.sessionId} ${item.summary || ''}`.toLowerCase().includes(keyword))
})

onMounted(async () => {
  await loadSessions()
})
</script>

<style scoped lang="scss">
.chat-shell {
  height: 100%;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr) 320px;
  gap: 16px;
}

.sidebar,
.workspace,
.logs-drawer {
  min-height: 0;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
}

.sidebar,
.logs-drawer {
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.workspace {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  padding: 20px;
  gap: 16px;
}

.logs-drawer {
  transition: width 0.2s ease, padding 0.2s ease;
  overflow: hidden;
}

.logs-drawer.collapsed {
  width: 72px;
  padding: 16px 8px;
}

.logs-drawer.collapsed .logs-drawer-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.logs-drawer.collapsed .logs-title-wrap,
.logs-drawer.collapsed .logs-actions {
  display: none;
}

.logs-drawer.collapsed::after {
  content: '日志';
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #8a919f;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 2px;
  writing-mode: vertical-rl;
  text-orientation: mixed;
}

.sidebar-header,
.workspace-header,
.logs-drawer-header,
.activity-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-title,
.workspace-title,
.logs-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2329;
}

.sidebar-subtitle,
.workspace-subtitle,
.logs-subtitle {
  font-size: 12px;
  color: #8a919f;
  margin-top: 4px;
}

.workspace-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-toolbar { margin: 16px 0 12px; }
.session-list,
.log-list { overflow: auto; min-height: 0; }

.session-card {
  width: 100%;
  border: 1px solid #ebeef5;
  background: #fff;
  border-radius: 14px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  margin-bottom: 10px;
  transition: all 0.2s ease;
}

.session-card:hover { border-color: #c6e2ff; transform: translateY(-1px); }
.session-card.active { border-color: #409eff; background: #f5faff; box-shadow: 0 8px 24px rgba(64, 158, 255, 0.12); }
.session-card-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.session-name { font-weight: 600; color: #1f2329; }
.session-card-sub { margin-top: 8px; color: #626a77; font-size: 13px; line-height: 1.5; }
.session-card-meta { margin-top: 10px; color: #8a919f; font-size: 12px; }

.message-panel {
  min-height: 0;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 16px;
  padding: 16px;
  background: linear-gradient(180deg, #fbfcff 0%, #f8fbff 100%);
}

.empty-state {
  padding: 56px 16px;
  text-align: center;
  color: #909399;
}
.empty-title { font-size: 20px; font-weight: 700; margin-bottom: 8px; color: #1f2329; }
.empty-subtitle { font-size: 13px; }

.message-item { display: flex; gap: 12px; margin: 14px 0; }
.message-item.user { flex-direction: row-reverse; }
.message-item.live .message-content { border-style: dashed; border-color: #f0b878; }
.message-avatar { width: 38px; height: 38px; border-radius: 50%; background: #e8f3ff; color: #409eff; display: flex; align-items: center; justify-content: center; font-weight: 700; flex-shrink: 0; }
.message-item.user .message-avatar { background: #fdf6ec; color: #e6a23c; }
.message-content { max-width: min(760px, calc(100% - 60px)); padding: 12px 14px; border-radius: 14px; background: #fff; border: 1px solid #ebeef5; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03); }
.message-item.user .message-content { background: #ecf5ff; }
.message-meta { font-size: 12px; color: #909399; margin-bottom: 6px; }
.message-text { white-space: pre-wrap; word-break: break-word; line-height: 1.75; color: #1f2329; }

.composer { display: flex; flex-direction: column; gap: 12px; }
.composer-actions { display: flex; justify-content: flex-end; gap: 10px; }

.log-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  margin-bottom: 12px;
  background: #fff;
}
.log-item.request { border-left: 4px solid #e6a23c; }
.log-item.success { border-left: 4px solid #67c23a; }
.log-item.error { border-left: 4px solid #f56c6c; }
.log-item.info { border-left: 4px solid #909399; }
.log-meta { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: #909399; margin-bottom: 8px; }
.log-content { white-space: pre-wrap; word-break: break-word; line-height: 1.6; color: #303133; }

.logs-drawer.collapsed .logs-drawer-header,
.logs-drawer.collapsed .log-list {
  display: none;
}

@media (max-width: 1400px) {
  .chat-shell { grid-template-columns: 280px minmax(0, 1fr) 300px; }
}

@media (max-width: 1200px) {
  .chat-shell { grid-template-columns: 1fr; }
  .sidebar { order: 2; }
  .workspace { order: 1; }
  .logs-drawer { order: 3; }
}
</style>

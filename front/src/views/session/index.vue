<template>
  <div class="session-shell">
    <section class="panel hero-panel">
      <div>
        <div class="hero-title">会话管理</div>
        <div class="hero-subtitle">查看线程历史、清理会话、排查对话上下文</div>
      </div>
      <el-tag type="info">Session</el-tag>
    </section>

    <section class="panel control-panel">
      <div class="panel-header">
        <div class="panel-title">线程查看</div>
        <el-button type="primary" plain @click="handleReset">重置</el-button>
      </div>

      <el-form :model="form" label-width="110px" class="form-grid">
        <el-form-item label="conversationId">
          <el-input v-model="form.conversationId" placeholder="请输入会话ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-space wrap>
            <el-button type="primary" :loading="loading.history" @click="handleLoadHistory">加载历史</el-button>
            <el-button type="danger" plain :loading="loading.clear" @click="handleClearSession">清除会话</el-button>
          </el-space>
        </el-form-item>
      </el-form>

      <el-alert v-if="message" :title="message" :type="messageType" show-icon :closable="false" />
    </section>

    <section class="panel history-panel">
      <div class="panel-header">
        <div class="panel-title">会话消息</div>
        <el-tag type="success">{{ history.length }} 条</el-tag>
      </div>

      <div class="message-list">
        <div v-for="(item, index) in history" :key="index" class="message-item" :class="item.role">
          <div class="message-role">{{ item.role }}</div>
          <div class="message-content">
            <div class="message-text">{{ item.content }}</div>
            <div class="message-meta">{{ item.createdAt || '-' }}</div>
          </div>
        </div>
        <el-empty v-if="!history.length" description="暂无会话历史" />
      </div>
    </section>

    <section class="panel monitor-panel">
      <div class="panel-header">
        <div class="panel-title">监控查询</div>
        <el-tag type="warning">/api/v1/monitor</el-tag>
      </div>

      <el-form :model="monitorForm" label-width="110px" class="form-grid">
        <el-form-item label="username">
          <el-input v-model="monitorForm.username" placeholder="可选，查询用户会话列表/历史" clearable />
        </el-form-item>
        <el-form-item label="pagination">
          <el-space>
            <el-input-number v-model="monitorForm.page" :min="0" />
            <el-input-number v-model="monitorForm.size" :min="1" :max="100" />
          </el-space>
        </el-form-item>
        <el-form-item>
          <el-space wrap>
            <el-button :loading="loading.monitorHistory" @click="handleLoadMonitorHistory">查询历史分页</el-button>
            <el-button :loading="loading.monitorSessions" @click="handleLoadUserSessions">查询用户会话列表</el-button>
            <el-button :loading="loading.monitorConversation" @click="handleLoadConversation">查询完整会话</el-button>
            <el-button type="primary" :loading="loading.stats" @click="handleLoadStats">查询统计</el-button>
          </el-space>
        </el-form-item>
      </el-form>
    </section>

    <section class="panel result-panel">
      <div class="panel-header">
        <div class="panel-title">查询结果</div>
        <el-button text size="small" @click="result = null">清空结果</el-button>
      </div>
      <div class="result-box">
        <template v-if="resultText">
          <pre>{{ resultText }}</pre>
        </template>
        <el-empty v-else description="暂无数据" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { clearSession, getSessionHistory } from '@/api/session'
import { getMonitorConversation, getMonitorHistory, getMonitorSessions, getMonitorStats } from '@/api/monitor'
import type { SessionHistoryItem } from '@/types/session'

const loading = reactive({ history: false, clear: false, stats: false, monitorHistory: false, monitorSessions: false, monitorConversation: false })
const form = reactive({ conversationId: '' })
const monitorForm = reactive({ username: '', page: 0, size: 20 })
const history = ref<SessionHistoryItem[]>([])
const result = ref<unknown>(null)
const message = ref('')
const messageType = ref<'success' | 'warning' | 'info' | 'error'>('info')

const resultText = computed(() => (result.value ? JSON.stringify(result.value, null, 2) : ''))

const setMessage = (text: string, type: 'success' | 'warning' | 'info' | 'error' = 'info') => {
  message.value = text
  messageType.value = type
}

const handleLoadHistory = async () => {
  if (!form.conversationId.trim()) {
    ElMessage.warning('请输入 conversationId')
    return
  }
  loading.history = true
  try {
    const res = await getSessionHistory(form.conversationId.trim())
    history.value = res || []
    setMessage(`加载到 ${history.value.length} 条会话消息`, 'success')
  } finally {
    loading.history = false
  }
}

const handleClearSession = async () => {
  if (!form.conversationId.trim()) {
    ElMessage.warning('请输入 conversationId')
    return
  }
  loading.clear = true
  try {
    await clearSession(form.conversationId.trim())
    history.value = []
    setMessage('会话已清除', 'success')
  } finally {
    loading.clear = false
  }
}

const handleLoadStats = async () => {
  loading.stats = true
  try {
    result.value = await getMonitorStats()
    setMessage('统计查询完成', 'success')
  } finally {
    loading.stats = false
  }
}

const handleLoadMonitorHistory = async () => {
  loading.monitorHistory = true
  try {
    result.value = await getMonitorHistory({ username: monitorForm.username.trim() || undefined, page: monitorForm.page, size: monitorForm.size })
    setMessage('分页历史查询完成', 'success')
  } finally {
    loading.monitorHistory = false
  }
}

const handleLoadUserSessions = async () => {
  if (!monitorForm.username.trim()) {
    ElMessage.warning('请输入 username')
    return
  }
  loading.monitorSessions = true
  try {
    result.value = await getMonitorSessions(monitorForm.username.trim())
    setMessage('用户会话列表查询完成', 'success')
  } finally {
    loading.monitorSessions = false
  }
}

const handleLoadConversation = async () => {
  if (!form.conversationId.trim()) {
    ElMessage.warning('请输入 conversationId')
    return
  }
  loading.monitorConversation = true
  try {
    result.value = await getMonitorConversation(form.conversationId.trim())
    setMessage('完整会话查询完成', 'success')
  } finally {
    loading.monitorConversation = false
  }
}

const handleReset = () => {
  form.conversationId = ''
  history.value = []
  message.value = ''
  result.value = null
}
</script>

<style scoped lang="scss">
.session-shell {
  height: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel {
  min-height: 0;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-panel {
  grid-column: 1 / -1;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #f7fbff 0%, #ffffff 100%);
}

.hero-title,
.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2329;
}

.hero-subtitle {
  margin-top: 4px;
  color: #8a919f;
  font-size: 13px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-grid {
  display: grid;
  gap: 4px;
}

.history-panel,
.result-panel {
  min-height: 0;
}

.message-list,
.result-box {
  min-height: 260px;
  overflow: auto;
}

.message-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  margin-bottom: 12px;
  background: #fff;
}

.message-item.user { border-left: 4px solid #e6a23c; }
.message-item.assistant { border-left: 4px solid #67c23a; }
.message-item.system { border-left: 4px solid #909399; }

.message-role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  text-transform: capitalize;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  color: #1f2329;
}

.message-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.result-box pre {
  white-space: pre-wrap;
  word-break: break-word;
  background: #fafafa;
  padding: 12px;
  border-radius: 10px;
}

@media (max-width: 1200px) {
  .session-shell { grid-template-columns: 1fr; }
  .hero-panel { grid-column: auto; }
}
</style>

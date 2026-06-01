<template>
  <div class="knowledge-page">
    <el-row :gutter="16" class="full-height">
      <el-col :span="5" class="side-col">
        <el-card shadow="never" class="panel-card left-panel">
          <template #header>
            <div class="card-header">
              <span>知识库管理</span>
              <el-tag type="info">文档 / 状态</el-tag>
            </div>
          </template>

          <div class="section-block">
            <div class="section-title">上传文档</div>
            <el-upload
              :auto-upload="false"
              :show-file-list="true"
              :limit="1"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
            >
              <el-button type="primary">选择文件</el-button>
            </el-upload>
            <el-button class="mt8" type="primary" plain :loading="loading.upload" @click="handleUpload">上传入库</el-button>
          </div>

          <div class="section-block">
            <div class="section-title">目录加载</div>
            <el-input v-model="form.dirPath" placeholder="如：src/main/resources/knowledge/" clearable />
            <el-button class="mt8" :loading="loading.dir" @click="handleLoadDirectory">批量加载目录</el-button>
          </div>

          <div class="section-block">
            <div class="section-title">知识库状态</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="文档数">{{ status.documentCount }}</el-descriptions-item>
              <el-descriptions-item label="切片数">{{ status.totalChunks }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="session-list-wrap">
            <div class="section-title between">
              <span>文档列表</span>
              <el-button text size="small" @click="handleRefresh">刷新</el-button>
            </div>
            <div class="doc-list">
              <div v-for="doc in documents" :key="doc.fileName" class="doc-item">
                <div class="doc-main">
                  <div class="doc-name">{{ doc.fileName }}</div>
                  <div class="doc-meta">{{ doc.fileSizeReadable }} · chunks {{ doc.chunkCount }}</div>
                </div>
                <el-button text type="danger" :loading="deletingFileName === doc.fileName" @click="handleDelete(doc.fileName)">删除</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="14" class="main-col">
        <el-card shadow="never" class="panel-card chat-panel">
          <template #header>
            <div class="card-header">
              <div>
                <span>知识库会话</span>
                <el-tag class="ml8" :type="statusTagType">{{ statusText }}</el-tag>
              </div>
              <div class="chat-header-meta">
                <el-tag type="success" effect="plain">Conversation: {{ form.conversationId || '-' }}</el-tag>
              </div>
            </div>
          </template>

          <div ref="chatBodyRef" class="chat-body">
            <div v-if="!messages.length && !liveAnswer" class="empty-state">
              <div class="empty-title">开始你的知识库问答</div>
              <div class="empty-subtitle">支持会话记忆、历史继续聊，并已接入流式输出</div>
            </div>

            <div v-for="item in messages" :key="item.id" class="message-item" :class="item.role">
              <div class="message-avatar">{{ item.role === 'user' ? '你' : item.role === 'assistant' ? 'AI' : 'i' }}</div>
              <div class="message-content">
                <div class="message-meta">{{ item.meta }}</div>
                <div class="message-text">{{ item.content }}</div>
              </div>
            </div>

            <div v-if="loading.ask && liveAnswer" class="message-item assistant live">
              <div class="message-avatar">AI</div>
              <div class="message-content">
                <div class="message-meta">流式生成中</div>
                <div class="message-text">{{ liveAnswer }}</div>
              </div>
            </div>
          </div>

          <div class="composer">
            <el-form :model="form" label-width="110px" class="composer-form">
              <el-form-item label="conversationId">
                <el-input v-model="form.conversationId" placeholder="可选，用于多轮追问" clearable />
              </el-form-item>
              <el-form-item label="问题">
                <el-input v-model="form.question" type="textarea" :rows="4" placeholder="请输入知识库问题" />
              </el-form-item>
              <el-form-item label="模式">
                <el-radio-group v-model="form.mode">
                  <el-radio-button label="auto">自动 RAG</el-radio-button>
                  <el-radio-button label="manual">手动 RAG</el-radio-button>
                  <el-radio-button label="search">纯检索</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="topK">
                <el-input-number v-model="form.topK" :min="1" :max="20" />
              </el-form-item>
              <el-form-item label="阈值">
                <el-input-number v-model="form.similarityThreshold" :min="0" :max="1" :step="0.05" />
              </el-form-item>
            </el-form>

            <div class="composer-actions">
              <el-button type="primary" :loading="loading.ask" @click="handleAsk">发送问题</el-button>
              <el-button :disabled="loading.ask" @click="handleStopStream">停止生成</el-button>
              <el-button @click="handleClearConversation">清空会话</el-button>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card mt16">
          <template #header>
            <div class="card-header">
              <span>问答结果</span>
              <el-tag type="warning">结果面板</el-tag>
            </div>
          </template>
          <div class="response-box">
            <template v-if="result || liveAnswer">
              <p><b>conversationId:</b> {{ normalizeSessionId(result) }}</p>
              <p><b>retrievedChunks:</b> {{ result?.retrievedChunks ?? '-' }}</p>
              <p><b>model:</b> {{ result?.model || '-' }}</p>
              <p><b>sources:</b> {{ (result?.sources || []).join('，') || '-' }}</p>
              <p><b>answer:</b></p>
              <pre>{{ normalizedAnswer }}</pre>
            </template>
            <el-empty v-else description="暂无结果" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="5" class="side-col">
        <el-card shadow="never" class="panel-card right-panel">
          <template #header>
            <div class="card-header">
              <span>请求日志</span>
              <el-button text size="small" @click="logs = []">清空</el-button>
            </div>
          </template>
          <div class="log-list">
            <div v-for="item in logs" :key="item.id" class="log-item" :class="item.type">
              <div class="log-meta">
                <span>{{ item.time }}</span>
                <el-tag size="small" :type="logTagType(item.type)">{{ item.type }}</el-tag>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile, type UploadFiles } from 'element-plus'
import { deleteDocument, getKnowledgeStatus, listDocuments, loadDirectory, searchKnowledge, uploadDocument } from '@/api/knowledge'
import type { KnowledgeDocumentVO, KnowledgeResponse, KnowledgeStatusResponse } from '@/types/knowledge'
import { useSseStream } from '@/composables/useSseStream'
import { normalizeAnswer, normalizeSessionId } from '@/utils/chatNormalize'

const loading = reactive({ ask: false, list: false, upload: false, dir: false })
const documents = ref<KnowledgeDocumentVO[]>([])
const status = reactive<KnowledgeStatusResponse>({ documentCount: 0, totalChunks: 0, documents: [] })
const result = ref<KnowledgeResponse | null>(null)
const selectedFile = ref<File | null>(null)
const deletingFileName = ref('')
const logs = ref<Array<{ id: string; type: 'request' | 'success' | 'error' | 'info'; time: string; content: string }>>([])
const messages = ref<Array<{ id: string; role: 'user' | 'assistant' | 'system'; content: string; meta: string }>>([])
const chatBodyRef = ref<HTMLElement | null>(null)

const { answer: liveAnswer, status: streamStatus, clear: clearStream, startFetchStream, stop: stopStream } = useSseStream()

const form = reactive({
  question: '',
  conversationId: '',
  dirPath: 'src/main/resources/knowledge/',
  topK: 5,
  similarityThreshold: 0.5,
  mode: 'auto' as 'auto' | 'manual' | 'search',
})

const normalizedAnswer = computed(() => liveAnswer.value || normalizeAnswer(result.value, '-'))
const statusText = computed(() => {
  if (loading.ask) return '生成中'
  if (streamStatus.value === 'streaming' || streamStatus.value === 'connecting') return '流式中'
  return '空闲'
})
const statusTagType = computed(() => {
  if (loading.ask || streamStatus.value === 'streaming' || streamStatus.value === 'connecting') return 'warning'
  return 'success'
})

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
  messages.value.push({
    id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    role,
    content,
    meta,
  })
  scrollToBottom()
}

const handleFileChange = (_uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  const raw = uploadFiles.at(-1)?.raw
  selectedFile.value = raw || null
}

const handleFileRemove = () => {
  selectedFile.value = null
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  loading.upload = true
  addLog('request', '上传文档到知识库')
  try {
    await uploadDocument(selectedFile.value)
    ElMessage.success('上传成功')
    addLog('success', '上传成功')
    selectedFile.value = null
    await handleRefresh()
  } catch (e: any) {
    addLog('error', e?.message || '上传失败')
    throw e
  } finally {
    loading.upload = false
  }
}

const handleLoadDirectory = async () => {
  loading.dir = true
  addLog('request', '批量加载目录')
  try {
    await loadDirectory(form.dirPath.trim() || undefined)
    ElMessage.success('目录加载完成')
    addLog('success', '目录加载完成')
    await handleRefresh()
  } catch (e: any) {
    addLog('error', e?.message || '目录加载失败')
    throw e
  } finally {
    loading.dir = false
  }
}

const handleRefresh = async () => {
  loading.list = true
  try {
    const [docsRes, statusRes] = await Promise.all([listDocuments(), getKnowledgeStatus()])
    documents.value = (docsRes || []) as KnowledgeDocumentVO[]
    Object.assign(status, statusRes || { documentCount: 0, totalChunks: 0, documents: [] })
  } catch (e: any) {
    addLog('error', e?.message || '刷新失败')
  } finally {
    loading.list = false
  }
}

const handleAsk = async () => {
  if (!form.question.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  loading.ask = true
  clearStream()
  result.value = null
  const question = form.question.trim()
  const conversationId = form.conversationId.trim() || undefined
  appendMessage('user', question, '用户')
  addLog('request', `知识库问答 mode=${form.mode}, conversationId=${conversationId || '-'}`)

  try {
    if (form.mode === 'search') {
      const res = await searchKnowledge(question)
      result.value = {
        conversationId,
        answer: typeof res === 'string' ? res : JSON.stringify(res, null, 2),
        sources: [],
        retrievedChunks: 0,
        model: 'search-only',
      }
      appendMessage('assistant', normalizeAnswer(result.value, '（空响应）'), `model: ${result.value?.model || '-'}`)
      addLog('success', `知识库响应完成 model=${result.value?.model || '-'}`)
      return
    }

    await startFetchStream({
      message: question,
      conversationId,
      mode: 'knowledge',
      endpoint: '/knowledge/stream',
      query: {
        topK: form.topK,
        similarityThreshold: form.similarityThreshold,
        manual: form.mode === 'manual',
      },
    })

    const finalAnswer = liveAnswer.value
    result.value = {
      conversationId,
      answer: finalAnswer,
      sources: [],
      retrievedChunks: 0,
      model: 'knowledge-stream',
    }
    liveAnswer.value = ''
    appendMessage('assistant', finalAnswer || '（空响应）', `model: ${result.value?.model || '-'}`)
    addLog('success', `知识库流式响应完成 model=${result.value?.model || '-'}`)
  } catch (e: any) {
    appendMessage('system', e?.message || '请求失败')
    addLog('error', e?.message || '请求失败')
  } finally {
    loading.ask = false
  }
}

const handleStopStream = () => {
  stopStream()
  addLog('info', '停止流式生成')
}

const handleClearConversation = () => {
  messages.value = []
  result.value = null
  form.question = ''
  clearStream()
  addLog('info', '清空知识库会话')
}

const handleDelete = async (fileName: string) => {
  await ElMessageBox.confirm(`确认删除 ${fileName} 吗？`, '提示', { type: 'warning' })
  deletingFileName.value = fileName
  addLog('request', `删除文档 ${fileName}`)
  try {
    await deleteDocument(fileName)
    ElMessage.success('删除成功')
    addLog('success', `删除成功 ${fileName}`)
    await handleRefresh()
  } catch (e: any) {
    addLog('error', e?.message || '删除失败')
    throw e
  } finally {
    deletingFileName.value = ''
  }
}

onMounted(() => {
  handleRefresh()
})
</script>

<style scoped lang="scss">
.knowledge-page {
  height: 100%;
}

.full-height {
  height: 100%;
}

.side-col,
.main-col {
  height: 100%;
}

.panel-card {
  height: 100%;
  border-radius: 12px;
}

.left-panel,
.right-panel,
.chat-panel {
  height: calc(100vh - 170px);
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.ml8 {
  margin-left: 8px;
}

.mt8 {
  margin-top: 8px;
}

.section-block {
  margin-bottom: 16px;
}

.section-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-list-wrap {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}

.doc-list,
.log-list {
  overflow: auto;
  min-height: 0;
  flex: 1;
}

.doc-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 10px;
  background: #fff;
}

.doc-name {
  font-weight: 600;
}

.doc-meta {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.chat-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 8px 4px;
  background: #fafcff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
}

.empty-state {
  padding: 40px 16px;
  text-align: center;
  color: #909399;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.empty-subtitle {
  font-size: 13px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin: 12px 0;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-item.live .message-content {
  border-style: dashed;
  border-color: #f0b878;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e8f3ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.message-item.user .message-avatar {
  background: #fdf6ec;
  color: #e6a23c;
}

.message-content {
  max-width: calc(100% - 60px);
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
}

.message-item.user .message-content {
  background: #ecf5ff;
}

.message-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.composer {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.composer-form {
  padding-right: 8px;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.response-box {
  min-height: 220px;
}

.response-box pre {
  white-space: pre-wrap;
  word-break: break-word;
  background: #fafafa;
  padding: 12px;
  border-radius: 8px;
}

.log-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fff;
}

.log-item.request {
  border-left: 4px solid #e6a23c;
}

.log-item.success {
  border-left: 4px solid #67c23a;
}

.log-item.error {
  border-left: 4px solid #f56c6c;
}

.log-item.info {
  border-left: 4px solid #909399;
}

.log-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.log-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}
</style>

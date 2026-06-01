<template>
  <div class="docs-page">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never" class="panel-card sticky-card">
          <template #header>
            <div class="header">接口文档目录</div>
          </template>

          <el-input v-model="keyword" placeholder="搜索接口、路径、模块" clearable />

          <el-menu class="module-menu" :default-active="activeModule" @select="activeModule = $event as string">
            <el-menu-item index="all">全部接口</el-menu-item>
            <el-menu-item v-for="m in modules" :key="m" :index="m">{{ m }}</el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <el-col :span="18">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="header">
              <span>后端接口文档 / 调试参考</span>
              <el-space>
                <el-tag type="info">Spring Boot</el-tag>
                <el-tag type="success">Vue 3</el-tag>
              </el-space>
            </div>
          </template>

          <el-alert
            title="本页用于前端开发时快速对照后端接口、请求方式、鉴权要求与适用页面。"
            type="info"
            show-icon
            :closable="false"
            class="mb16"
          />

          <el-table :data="filteredDocs" stripe border height="760">
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="method" label="方法" width="90" />
            <el-table-column prop="path" label="路径" min-width="220" />
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="description" label="说明" min-width="260" />
            <el-table-column prop="request" label="请求体" min-width="220" />
            <el-table-column prop="response" label="响应体" min-width="220" />
            <el-table-column prop="auth" label="鉴权" width="90">
              <template #default="scope">
                <el-tag :type="scope.row.auth ? 'warning' : 'success'">{{ scope.row.auth ? '需要' : '公开' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ApiDocItem } from '@/types/docs'

const keyword = ref('')
const activeModule = ref('all')

const docs: ApiDocItem[] = [
  { module: 'auth', method: 'POST', path: '/api/v1/auth/login', title: '用户登录', description: '使用用户名密码登录并获取 JWT', request: 'LoginRequest', response: 'R<AuthResponse>', auth: false },
  { module: 'auth', method: 'POST', path: '/api/v1/auth/register', title: '用户注册', description: '注册用户并自动返回 token', request: 'RegisterRequest', response: 'R<AuthResponse>', auth: false },
  { module: 'auth', method: 'POST', path: '/api/v1/auth/init-admin', title: '初始化管理员', description: '创建默认管理员账号 admin / admin123', response: 'R<AuthResponse>', auth: false },

  { module: 'chat', method: 'POST', path: '/api/v1/chat/memory', title: '记忆对话', description: '同 conversationId 具备上下文记忆', request: 'ChatRequest', response: 'R<ChatResponse>', auth: true },
  { module: 'chat', method: 'GET', path: '/api/v1/chat/memory/stream?message=xxx', title: '记忆流式对话', description: '实时推送带会话记忆的回复', response: 'Flux<String>', auth: true },
  { module: 'chat', method: 'GET', path: '/api/v1/chat/ping', title: '健康检查', description: '检查服务是否可用', response: 'R<String>', auth: true },

  { module: 'agent', method: 'POST', path: '/api/v1/agent/chat', title: '工具调用对话', description: 'AI 自主决定是否调用工具', request: 'AgentRequest', response: 'R<AgentResponse>', auth: true },
  { module: 'agent', method: 'POST', path: '/api/v1/agent/chat/memory', title: '工具调用+记忆', description: '带上下文工具调用', request: 'AgentRequest', response: 'R<AgentResponse>', auth: true },
  { module: 'agent', method: 'POST', path: '/api/v1/agent/chat/specific', title: '指定工具对话', description: '只启用请求中指定的工具', request: 'AgentRequest', response: 'R<AgentResponse>', auth: true },

  { module: 'knowledge', method: 'POST', path: '/api/v1/knowledge/upload', title: '上传文档', description: '上传 txt/md 并入库', request: 'Multipart file', response: 'R<DocumentVO>', auth: true },
  { module: 'knowledge', method: 'POST', path: '/api/v1/knowledge/load-directory', title: '加载目录', description: '批量加载知识库目录', request: 'path query param', response: 'R<List<DocumentVO>>', auth: true },
  { module: 'knowledge', method: 'GET', path: '/api/v1/knowledge/documents', title: '文档列表', description: '列出已入库文档', response: 'R<List<DocumentVO>>', auth: true },
  { module: 'knowledge', method: 'DELETE', path: '/api/v1/knowledge/documents/{fileName}', title: '删除文档', description: '按文件名删除知识库文档', response: 'R<String>', auth: true },
  { module: 'knowledge', method: 'POST', path: '/api/v1/knowledge/ask', title: '自动 RAG 问答', description: 'QuestionAnswerAdvisor 自动问答', request: 'KnowledgeRequest', response: 'R<KnowledgeResponse>', auth: true },
  { module: 'knowledge', method: 'POST', path: '/api/v1/knowledge/ask/manual', title: '手动 RAG 问答', description: '手动控制检索与拼接', request: 'KnowledgeRequest', response: 'R<KnowledgeResponse>', auth: true },
  { module: 'knowledge', method: 'GET', path: '/api/v1/knowledge/search', title: '纯检索', description: '不生成回答，仅返回检索结果', response: 'R<String>', auth: true },
  { module: 'knowledge', method: 'GET', path: '/api/v1/knowledge/status', title: '知识库状态', description: '返回文档数、chunk 数等状态', response: 'R<Map<String,Object>>', auth: true },

  { module: 'planning', method: 'POST', path: '/api/v1/planning/execute', title: '规划并执行', description: 'AI 自动规划并执行任务', request: 'PlanningRequest', response: 'R<PlanningResponse>', auth: true },
  { module: 'planning', method: 'POST', path: '/api/v1/planning/plan-only', title: '仅规划', description: '只返回拆解计划', request: 'PlanningRequest', response: 'R<PlanningResponse>', auth: true },
  { module: 'planning', method: 'POST', path: '/api/v1/planning/chat', title: '全能 Agent 入口', description: '整合记忆与工具的统一入口', request: 'AgentRequest', response: 'R<AgentResponse>', auth: true },

  { module: 'stream', method: 'GET', path: '/api/v1/stream/chat', title: 'SSE 基础流式对话', description: '实时推送 AI 回复', response: 'Flux<String>', auth: true },
  { module: 'stream', method: 'GET', path: '/api/v1/stream/chat/tools', title: 'SSE 工具流式对话', description: '流式输出中允许工具调用', response: 'Flux<String>', auth: true },

  { module: 'session', method: 'GET', path: '/api/v1/session/{conversationId}/history', title: '会话历史', description: '获取会话历史消息', response: 'R<List<Map<String,String>>>', auth: true },
  { module: 'session', method: 'DELETE', path: '/api/v1/session/{conversationId}', title: '清除会话', description: '删除某个会话的历史', response: 'R<String>', auth: true },

  { module: 'monitor', method: 'GET', path: '/api/v1/monitor/stats', title: '统计概览', description: '系统会话、用户等统计信息', response: 'R<Map<String,Object>>', auth: true },
  { module: 'monitor', method: 'GET', path: '/api/v1/monitor/history', title: '历史分页', description: '分页查看聊天历史', response: 'R<Page<ChatHistoryEntity>>', auth: true },
  { module: 'monitor', method: 'GET', path: '/api/v1/monitor/conversation/{conversationId}', title: '完整会话', description: '查看某会话完整内容', response: 'R<List<ChatHistoryEntity>>', auth: true },
  { module: 'monitor', method: 'GET', path: '/api/v1/monitor/sessions/{username}', title: '用户会话列表', description: '查看某用户的会话列表', response: 'R<List<String>>', auth: true },
]

const modules = Array.from(new Set(docs.map((d) => d.module)))

const filteredDocs = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return docs.filter((doc) => {
    const moduleOk = activeModule.value === 'all' || doc.module === activeModule.value
    const keywordOk =
      !k ||
      [doc.module, doc.method, doc.path, doc.title, doc.description, doc.request || '', doc.response || '']
        .join(' ')
        .toLowerCase()
        .includes(k)
    return moduleOk && keywordOk
  })
})
</script>

<style scoped lang="scss">
.docs-page {
  width: 100%;
}

.panel-card {
  border-radius: 12px;
}

.sticky-card {
  position: sticky;
  top: 20px;
}

.header {
  font-weight: 600;
}

.mb16 {
  margin-bottom: 16px;
}

.module-menu {
  border-right: none;
  margin-top: 12px;
}
</style>

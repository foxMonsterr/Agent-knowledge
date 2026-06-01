<template>
  <div class="page-wrap">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>审计日志</span>
          <el-input v-model="conversationId" placeholder="按 conversationId 过滤（可选）" style="max-width: 280px" clearable />
          <el-button type="primary" plain @click="load">查询</el-button>
        </div>
      </template>

      <el-table :data="logs" stripe>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="traceId" label="TraceId" width="220" />
        <el-table-column prop="conversationId" label="会话ID" width="180" />
        <el-table-column prop="agentType" label="Agent" width="120" />
        <el-table-column prop="model" label="模型" width="140" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="latencyMs" label="耗时(ms)" width="100" />
        <el-table-column prop="input" label="输入" />
        <el-table-column prop="output" label="输出" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getAuditLogs, type AuditLogVO } from '@/api/audit'

const logs = ref<AuditLogVO[]>([])
const conversationId = ref('')

const load = async () => {
  logs.value = await getAuditLogs(conversationId.value.trim() || undefined)
}

onMounted(load)
</script>

<style scoped>
.page-wrap { padding: 12px; }
.header { display: flex; gap: 12px; align-items: center; }
</style>

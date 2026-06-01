<template>
  <div class="page-wrap">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">总用户数</div>
          <div class="stat-value">{{ overview?.totalUsers ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">聊天记录</div>
          <div class="stat-value">{{ overview?.totalChatRecords ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">Agent 调用</div>
          <div class="stat-value">{{ overview?.totalAgentRuns ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>系统概览</template>
          <el-table :data="dashboard?.metrics || []" stripe>
            <el-table-column prop="title" label="指标" width="120" />
            <el-table-column prop="value" label="数值" width="120" />
            <el-table-column prop="subTitle" label="说明" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>最近活动</template>
          <el-table :data="dashboard?.recentAudits || []" stripe height="300">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="agentType" label="Agent" width="100" />
            <el-table-column prop="status" label="状态" width="80" />
            <el-table-column prop="traceId" label="TraceId" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>聊天趋势</template>
          <pre>{{ dashboard?.dailyChats }}</pre>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>Agent 趋势</template>
          <pre>{{ dashboard?.dailyRuns }}</pre>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>Token 趋势</template>
          <pre>{{ dashboard?.dailyTokens }}</pre>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>性能摘要</template>
          <el-row :gutter="16">
            <el-col :span="6"><div class="perf-item">请求成功率：{{ performance?.requestSuccessRate ?? 0 }}</div></el-col>
            <el-col :span="6"><div class="perf-item">Agent 成功率：{{ performance?.agentSuccessRate ?? 0 }}</div></el-col>
            <el-col :span="6"><div class="perf-item">RAG 命中率：{{ performance?.ragHitRate ?? 0 }}</div></el-col>
            <el-col :span="6"><div class="perf-item">平均耗时：{{ performance?.avgLatencyMs ?? 0 }}ms</div></el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getMonitorOverview, getMonitorStats } from '@/api/monitor'
import { getOpsDashboard } from '@/api/ops'
import { getPerformanceSummary } from '@/api/performance'

const overview = ref<any>(null)
const stats = ref<any>(null)
const dashboard = ref<any>(null)
const performance = ref<any>(null)

const load = async () => {
  overview.value = await getMonitorOverview()
  stats.value = await getMonitorStats()
  dashboard.value = await getOpsDashboard()
  performance.value = await getPerformanceSummary()
}

onMounted(load)
</script>

<style scoped>
.page-wrap { padding: 12px; }
.mt16 { margin-top: 16px; }
.stat-card { text-align: center; }
.stat-title { color: #909399; }
.stat-value { font-size: 32px; font-weight: 700; margin-top: 8px; }
.perf-item { padding: 12px; background: #f8fafc; border-radius: 8px; }
</style>

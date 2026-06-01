<template>
  <div class="page-wrap">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">系统状态</div>
          <div class="stat-value">{{ health?.status || 'UNKNOWN' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">TraceId</div>
          <div class="stat-value small">{{ health?.traceId || '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">服务</div>
          <div class="stat-value">{{ health?.service || 'smart-agent' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>健康检查</template>
          <pre>{{ health }}</pre>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>监控概览</template>
          <pre>{{ overview }}</pre>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHealth } from '@/api/health'
import { getMonitorOverview } from '@/api/monitor'

const health = ref<any>(null)
const overview = ref<any>(null)

const load = async () => {
  health.value = await getHealth()
  overview.value = await getMonitorOverview()
}

onMounted(load)
</script>

<style scoped>
.page-wrap { padding: 12px; }
.mt16 { margin-top: 16px; }
.stat-card { text-align: center; }
.stat-title { color: #909399; }
.stat-value { font-size: 28px; font-weight: 700; margin-top: 8px; }
.small { font-size: 12px; word-break: break-all; }
</style>

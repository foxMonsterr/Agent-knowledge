<template>
  <div class="home-page">
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never" class="hero-card">
          <div class="hero-title">{{ overview?.title || '企业级 AI 智能体平台' }}</div>
          <div class="hero-desc">{{ overview?.subtitle || '多 Agent 编排 · RAG 知识库 · 工具调用 · 审计监控' }}</div>
          <el-space wrap class="mt16">
            <el-tag type="success">JWT + RBAC</el-tag>
            <el-tag type="warning">TraceId</el-tag>
            <el-tag type="info">SSE</el-tag>
          </el-space>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="hero-card">
          <div class="hero-title">项目亮点</div>
          <ul class="bullet-list">
            <li v-for="item in overview?.highlights || defaultHighlights" :key="item">{{ item }}</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col v-for="metric in overview?.metrics || metrics" :key="metric.title" :span="6">
        <el-card shadow="never" class="metric-card">
          <div class="metric-title">{{ metric.title }}</div>
          <div class="metric-value">{{ metric.value }}</div>
          <div class="metric-subtitle">{{ metric.subTitle }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="24">
        <el-card shadow="never" class="flow-card">
          <template #header>
            <div class="flow-header">
              <span>推荐演示流程</span>
              <el-button type="primary" plain @click="$router.push('/demo')">进入演示中心</el-button>
            </div>
          </template>
          <el-steps :active="5" align-center>
            <el-step v-for="step in flowSteps" :key="step.title" :title="step.title" :description="step.highlight" />
          </el-steps>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHomeOverview, type HomeOverviewVO } from '@/api/home'
import { getDemoFlow, type DemoFlowStepVO } from '@/api/demo'
import type { OpsMetricVO } from '@/api/ops'

const overview = ref<HomeOverviewVO | null>(null)
const metrics = ref<OpsMetricVO[]>([])
const flowSteps = ref<DemoFlowStepVO[]>([])
const defaultHighlights = [
  'JWT + RBAC 权限体系',
  'TraceId 全链路追踪',
  '支持 Docker 部署',
  '前后端分离控制台',
]

const load = async () => {
  overview.value = await getHomeOverview()
  metrics.value = overview.value?.metrics || []
  flowSteps.value = await getDemoFlow()
}

onMounted(load)
</script>

<style scoped>
.home-page { padding: 12px; }
.hero-card, .metric-card, .flow-card { min-height: 240px; border-radius: 12px; }
.hero-title { font-size: 20px; font-weight: 700; margin-bottom: 8px; }
.hero-desc { color: #606266; line-height: 1.8; }
.mt16 { margin-top: 16px; }
.bullet-list { margin: 12px 0 0; padding-left: 18px; color: #4b5563; line-height: 2; }
.metric-card { text-align: center; }
.metric-title { color: #909399; }
.metric-value { font-size: 28px; font-weight: 700; margin-top: 8px; }
.metric-subtitle { color: #909399; margin-top: 6px; }
.flow-header { display: flex; justify-content: space-between; align-items: center; }
</style>

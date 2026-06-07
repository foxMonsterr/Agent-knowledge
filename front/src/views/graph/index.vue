<template>
  <el-card shadow="never">
    <template #header>
      <div class="header">
        <span>知识图谱</span>
        <el-button @click="load">刷新</el-button>
      </div>
    </template>

    <template v-if="graph.summary">
      <div class="graph-summary">
        <el-row :gutter="16">
          <el-col :span="4" v-for="item in summaryCards" :key="item.label">
            <el-card shadow="never" class="summary-card">
              <div class="summary-label">{{ item.label }}</div>
              <div class="summary-value" :class="item.tone">{{ item.value }}</div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      <el-divider />
    </template>

    <div class="graph-stats">
      <el-statistic title="节点数" :value="graph.nodes?.length || 0" />
      <el-statistic title="边数" :value="graph.edges?.length || 0" />
    </div>
    <div class="nodes">
      <el-tag v-for="node in graph.nodes || []" :key="node.id" class="node-tag" :type="node.colorGroup === 'weak' ? 'danger' : node.colorGroup === 'mastered' ? 'success' : 'info'">
        {{ node.label }} ({{ node.masteryLevel || 0 }}%)
      </el-tag>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getKnowledgeGraph } from '@/api/learn'

const graph = ref<any>({})

const summaryCards = computed(() => {
  const s = graph.value.summary
  if (!s) return []
  return [
    { label: '已掌握', value: s.masteredCount || 0, tone: 'tone-strong' },
    { label: '学习中', value: s.learningCount || 0, tone: 'tone-mid' },
    { label: '薄弱', value: s.weakCount || 0, tone: 'tone-weak' },
    { label: '新节点', value: s.newCount || 0, tone: 'tone-new' },
    { label: '平均掌握度', value: (s.averageMastery || 0) + '%', tone: 'tone-avg' },
  ]
})

const load = async () => { graph.value = await getKnowledgeGraph() }
onMounted(load)
</script>

<style scoped>
.header, .graph-stats { display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.nodes { margin-top: 24px; line-height: 2.5; }
.node-tag { margin-right: 8px; }
.summary-card { text-align: center; border-radius: 12px; }
.summary-label { color: #909399; font-size: 12px; }
.summary-value { margin-top: 8px; font-size: 24px; font-weight: 800; }
.tone-strong { color: #16a34a; }
.tone-mid { color: #2563eb; }
.tone-weak { color: #dc2626; }
.tone-new { color: #9333ea; }
.tone-avg { color: #0f766e; }
</style>

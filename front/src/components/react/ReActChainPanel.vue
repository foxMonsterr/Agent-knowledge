<template>
  <el-card shadow="never" class="react-chain-panel">
    <template #header>
      <div class="header">
        <span>{{ title }}</span>
        <el-tag :type="statusType">{{ status }}</el-tag>
      </div>
    </template>

    <div v-if="!steps.length" class="empty">
      {{ emptyText }}
    </div>

    <el-timeline v-else>
      <el-timeline-item
        v-for="step in steps"
        :key="`${step.type}-${step.stepNumber}-${step.actionName || ''}`"
        :timestamp="`Step ${step.stepNumber}`"
      >
        <div class="step-card" :class="step.status">
          <div class="step-header">
            <span class="step-type">{{ step.type }}</span>
            <el-tag v-if="step.status" size="small" effect="plain">{{ step.status }}</el-tag>
          </div>
          <div v-if="step.thought" class="step-text">{{ step.thought }}</div>
          <div v-if="step.actionName" class="step-text">工具：{{ step.actionName }}</div>
          <pre v-if="step.actionInput">{{ formatInput(step.actionInput) }}</pre>
          <div v-if="step.observation" class="step-text">{{ step.observation }}</div>
          <div v-if="step.durationMs !== undefined" class="duration">耗时 {{ step.durationMs }}ms</div>
          <div v-if="step.errorMessage" class="error-text">{{ step.errorMessage }}</div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ReActStep } from '@/types/react'

const props = withDefaults(defineProps<{
  title?: string
  status: string
  steps: ReActStep[]
  emptyText?: string
}>(), {
  title: 'ReAct 推理链路',
  emptyText: '发送问题后，这里会显示 Thought / Action / Observation。',
})

const statusType = computed(() => {
  if (props.status === 'completed') return 'success'
  if (props.status === 'error') return 'danger'
  if (props.status === 'streaming' || props.status === 'connecting') return 'warning'
  return 'info'
})

const formatInput = (value: Record<string, unknown>) => {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}
</script>

<style scoped lang="scss">
.react-chain-panel {
  height: 100%;
  border-radius: 22px;
}

.react-chain-panel :deep(.el-card__body) {
  height: calc(100% - 58px);
  overflow: auto;
  padding: 16px 18px 18px;
}

.react-chain-panel :deep(.el-timeline) {
  padding-left: 2px;
}

.react-chain-panel :deep(.el-timeline-item__node) {
  background-color: var(--la-accent);
}

.react-chain-panel :deep(.el-timeline-item__tail) {
  border-left-color: rgba(15, 118, 110, 0.2);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 850;
  letter-spacing: -0.02em;
}

.empty {
  min-height: 180px;
  display: grid;
  place-items: center;
  padding: 18px;
  border: 1px dashed rgba(15, 118, 110, 0.28);
  border-radius: 18px;
  color: var(--la-ink-muted);
  line-height: 1.8;
  text-align: center;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(236, 248, 244, 0.66));
}

.step-card {
  border: 1px solid rgba(190, 211, 202, 0.78);
  border-radius: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.88);
  line-height: 1.7;
  box-shadow: 0 10px 24px rgba(16, 44, 39, 0.05);
}

.step-card.failed {
  border-color: rgba(181, 75, 59, 0.42);
  background: #fff7f5;
}

.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.step-type {
  color: var(--la-accent-strong);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.06em;
}

.step-text {
  white-space: pre-wrap;
  word-break: break-word;
  color: #263c36;
}

.duration {
  color: var(--la-ink-muted);
  font-size: 12px;
  margin-top: 8px;
}

.error-text {
  color: #b54b3b;
  margin-top: 6px;
}

pre {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 8px 0 0;
  padding: 10px;
  border-radius: 12px;
  color: #18352f;
  background: #edf8f5;
  border: 1px solid rgba(15, 118, 110, 0.12);
}
</style>

<template>
  <div class="page-wrap">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>演示中心</span>
          <el-tag type="success">Demo Flow</el-tag>
        </div>
      </template>

      <el-steps direction="vertical" :active="999">
        <el-step v-for="step in steps" :key="step.title" :title="step.title" :description="step.description + ' · ' + step.highlight">
          <template #icon>
            <el-button type="primary" text @click="$router.push(step.route)">前往</el-button>
          </template>
        </el-step>
      </el-steps>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getDemoFlow, type DemoFlowStepVO } from '@/api/demo'

const steps = ref<DemoFlowStepVO[]>([])

onMounted(async () => {
  steps.value = await getDemoFlow()
})
</script>

<style scoped>
.page-wrap { padding: 12px; }
.header { display: flex; justify-content: space-between; align-items: center; }
</style>

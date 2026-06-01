<template>
  <div class="page-wrap">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>发布清单</template>
          <el-result v-if="checklist?.status" :title="checklist.status" icon="success" />
          <ul class="list">
            <li v-for="item in checklist?.items || []" :key="item">{{ item }}</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>发布摘要</template>
          <el-result v-if="summary?.status" :title="summary.status" icon="success" />
          <div class="summary-title">{{ summary?.title }}</div>
          <ul class="list">
            <li v-for="item in summary?.highlights || []" :key="item">{{ item }}</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getReleaseChecklist, getReleaseSummary } from '@/api/release'

const checklist = ref<any>(null)
const summary = ref<any>(null)

onMounted(async () => {
  checklist.value = await getReleaseChecklist()
  summary.value = await getReleaseSummary()
})
</script>

<style scoped>
.page-wrap { padding: 12px; }
.list { padding-left: 18px; line-height: 2; color: #374151; }
.summary-title { margin: 12px 0; font-weight: 700; font-size: 18px; }
</style>

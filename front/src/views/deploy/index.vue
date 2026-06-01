<template>
  <div class="page-wrap">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>部署信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="应用名">{{ info?.appName }}</el-descriptions-item>
            <el-descriptions-item label="环境">{{ info?.profile }}</el-descriptions-item>
            <el-descriptions-item label="Java">{{ info?.javaVersion }}</el-descriptions-item>
            <el-descriptions-item label="系统">{{ info?.osName }}</el-descriptions-item>
            <el-descriptions-item label="TraceId">{{ info?.traceId }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ info?.status }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>部署自检</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="数据库">{{ check?.database }}</el-descriptions-item>
            <el-descriptions-item label="Redis">{{ check?.redis }}</el-descriptions-item>
            <el-descriptions-item label="Health">{{ check?.health }}</el-descriptions-item>
            <el-descriptions-item label="前端代理">{{ check?.frontendProxy }}</el-descriptions-item>
            <el-descriptions-item label="TraceId">{{ check?.traceId }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getDeployCheck, getDeployInfo } from '@/api/deploy'

const info = ref<any>(null)
const check = ref<any>(null)

onMounted(async () => {
  info.value = await getDeployInfo()
  check.value = await getDeployCheck()
})
</script>

<style scoped>
.page-wrap { padding: 12px; }
</style>

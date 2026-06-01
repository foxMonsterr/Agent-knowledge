<template>
  <div class="redirect-page">
    <el-result
      icon="info"
      title="对话管理已整合到基础对话"
      sub-title="该页面已迁移到 /stream，正在为你跳转..."
    >
      <template #extra>
        <el-button type="primary" @click="goStream">前往基础对话</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const goStream = () => {
  router.replace('/stream')
}

onMounted(() => {
  goStream()
})
</script>

<style scoped lang="scss">
.redirect-page {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="always">
      <div class="title">初始化管理员</div>
      <div class="subtitle">调用后端 `/api/v1/auth/init-admin` 创建默认管理员</div>

      <el-alert
        title="此接口通常仅用于开发环境或首次部署"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />

      <el-button type="primary" class="block-btn" :loading="loading" @click="handleInit">初始化管理员</el-button>

      <div class="links">
        <router-link to="/login">返回登录</router-link>
        <router-link to="/register">去注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { initAdmin } from '@/api/auth'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const handleInit = async () => {
  loading.value = true
  try {
    const auth = await initAdmin()
    if (!auth || !auth.token) {
      ElMessage.error('初始化失败，返回数据为空或缺少 token')
      return
    }
    userStore.setAuth(auth)
    ElMessage.success('管理员初始化成功')
    router.push('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-page {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef4ff 0%, #f7faff 100%);
}

.auth-card {
  width: 420px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  text-align: center;
}

.subtitle {
  margin: 8px 0 24px;
  text-align: center;
  color: #6b7280;
}

.block-btn {
  width: 100%;
}

.links {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 14px;
}
</style>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="always">
      <div class="title">AI Agent 管理台</div>
      <div class="subtitle">登录到 Spring Boot 后端</div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>

        <el-button type="primary" class="block-btn" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>

      <div class="links">
        <router-link to="/register">注册账号</router-link>
        <router-link to="/init-admin">初始化管理员</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return

  loading.value = true
  try {
    const auth = await login(form)
    if (!auth || !auth.token) {
      ElMessage.error('登录失败，返回数据为空或缺少 token')
      return
    }
    userStore.setAuth(auth)
    ElMessage.success('登录成功')
    await router.push('/home')
  } catch (error) {
    console.error('[login] failed:', error)
    ElMessage.error('登录流程失败，请查看控制台日志')
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

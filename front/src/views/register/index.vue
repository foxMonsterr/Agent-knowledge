<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="always">
      <div class="title">用户注册</div>
      <div class="subtitle">创建一个用于联调的测试账号</div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleRegister">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-50 个字符" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-100 个字符" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="可选" />
        </el-form-item>

        <el-button type="primary" class="block-btn" :loading="loading" @click="handleRegister">注册并登录</el-button>
      </el-form>

      <div class="links">
        <router-link to="/login">返回登录</router-link>
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
import { register } from '@/api/auth'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度 6-100 个字符', trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    const res = await register(form)
    userStore.setAuth(res)
    ElMessage.success('注册成功')
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

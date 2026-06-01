import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/modules/user'
import type { ApiResponse } from '@/types/auth'

// 统一使用 /api/v1 作为代理前缀，env 文件不配置时兜底使用 /api/v1。
const envBaseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const request: AxiosInstance = axios.create({
  baseURL: envBaseURL,
  timeout: 30000,
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 200) return res.data
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response.data
  },
  (error: AxiosError<any>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络异常'

    if (status === 401) {
      const userStore = useUserStore()
      userStore.clearUser()
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
      ElMessage.error('登录已失效，请重新登录')
      return Promise.reject(error)
    }

    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default request

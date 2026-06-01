import request from '@/utils/request'
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types/auth'

export const login = (data: LoginRequest) => {
  return request.post<AuthResponse>('/auth/login', data)
}

export const register = (data: RegisterRequest) => {
  return request.post<AuthResponse>('/auth/register', data)
}

export const initAdmin = () => {
  return request.post<AuthResponse>('/auth/init-admin')
}

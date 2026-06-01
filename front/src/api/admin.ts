import request from '@/utils/request'

export interface UserVO {
  id: number
  username: string
  nickname?: string
  role: string
  enabled: boolean
  lastLoginAt?: string
  createdAt?: string
}

export const getUserList = () => request.get<UserVO[]>('/admin/users')
export const toggleUserEnabled = (id: number, enabled: boolean) =>
  request.patch<UserVO>(`/admin/users/${id}/enabled`, null, { params: { enabled } })
export const changeUserRole = (id: number, role: string) =>
  request.patch<UserVO>(`/admin/users/${id}/role`, null, { params: { role } })

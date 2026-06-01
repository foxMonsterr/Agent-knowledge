import request from '@/utils/request'

export interface PermissionVO {
  role: string
  menus: string[]
  actions: string[]
}

export const getCurrentPermission = () => {
  return request.get<PermissionVO>('/permission/current')
}

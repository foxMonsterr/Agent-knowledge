import { defineStore } from 'pinia'
import type { PermissionVO } from '@/api/permission'
import { getCurrentPermission } from '@/api/permission'

interface PermissionState {
  role: string
  menus: string[]
  actions: string[]
  loaded: boolean
}

export const usePermissionStore = defineStore('permission', {
  state: (): PermissionState => ({
    role: '',
    menus: [],
    actions: [],
    loaded: false,
  }),
  getters: {
    hasMenu: (state) => (menu: string) => state.menus.includes(menu),
    hasAction: (state) => (action: string) => state.actions.includes(action),
    isAdmin: (state) => state.role === 'ROLE_ADMIN',
  },
  actions: {
    async loadPermission() {
      const data = await getCurrentPermission()
      this.role = data.role
      this.menus = data.menus || []
      this.actions = data.actions || []
      this.loaded = true
    },
    clearPermission() {
      this.role = ''
      this.menus = []
      this.actions = []
      this.loaded = false
    },
  },
})

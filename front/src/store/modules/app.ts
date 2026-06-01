import { defineStore } from 'pinia'

interface AppState {
  sidebarCollapse: boolean
  pageLoading: boolean
}

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    sidebarCollapse: false,
    pageLoading: false,
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapse = !this.sidebarCollapse
    },
    setPageLoading(loading: boolean) {
      this.pageLoading = loading
    },
  },
})

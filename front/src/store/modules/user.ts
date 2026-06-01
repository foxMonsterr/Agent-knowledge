import { defineStore } from 'pinia'

export interface UserInfo {
  username: string
  role: string
  nickname?: string
}

interface UserState {
  token: string
  username: string
  role: string
  nickname: string
}

const TOKEN_KEY = 'AI_AGENT_TOKEN'
const USERNAME_KEY = 'AI_AGENT_USERNAME'
const ROLE_KEY = 'AI_AGENT_ROLE'
const NICKNAME_KEY = 'AI_AGENT_NICKNAME'

function readStorage(key: string) {
  return localStorage.getItem(key) || ''
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: readStorage(TOKEN_KEY),
    username: readStorage(USERNAME_KEY),
    role: readStorage(ROLE_KEY),
    nickname: readStorage(NICKNAME_KEY),
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
    },
    setUserInfo(payload: UserInfo) {
      this.username = payload.username
      this.role = payload.role
      this.nickname = payload.nickname || ''
      localStorage.setItem(USERNAME_KEY, payload.username)
      localStorage.setItem(ROLE_KEY, payload.role)
      localStorage.setItem(NICKNAME_KEY, payload.nickname || '')
    },
    setAuth(payload: { token: string; username: string; role: string; nickname?: string }) {
      this.setToken(payload.token)
      this.setUserInfo(payload)
    },
    clearUser() {
      this.token = ''
      this.username = ''
      this.role = ''
      this.nickname = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USERNAME_KEY)
      localStorage.removeItem(ROLE_KEY)
      localStorage.removeItem(NICKNAME_KEY)
    },
  },
})

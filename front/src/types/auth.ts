export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: string
}

export interface AuthResponse {
  token: string
  username: string
  role: string
  nickname?: string
  expiresIn?: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
}

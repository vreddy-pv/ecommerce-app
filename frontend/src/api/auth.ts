import client from './client'
import type { ApiResponse, AuthResponse } from '../types'

export const login = (username: string, password: string) =>
  client
    .post<ApiResponse<AuthResponse>>('/auth/login', { username, password })
    .then((r) => r.data.data)

export const register = (username: string, email: string, password: string) =>
  client
    .post<ApiResponse<AuthResponse>>('/auth/register', { username, email, password })
    .then((r) => r.data.data)

export const logout = (refreshToken: string) =>
  client.post('/auth/logout', { refreshToken })

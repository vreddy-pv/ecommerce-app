import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import type { AuthResponse } from '../types'

interface AuthState {
  user: { username: string; role: string } | null
  isAuthenticated: boolean
}

interface AuthContextValue extends AuthState {
  login: (username: string, password: string) => Promise<void>
  register: (username: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(() => {
    const token = localStorage.getItem('accessToken')
    const username = localStorage.getItem('username')
    const role = localStorage.getItem('role')
    return token && username && role
      ? { user: { username, role }, isAuthenticated: true }
      : { user: null, isAuthenticated: false }
  })

  const storeAuth = (res: AuthResponse) => {
    localStorage.setItem('accessToken', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    localStorage.setItem('username', res.username)
    localStorage.setItem('role', res.role)
    setState({ user: { username: res.username, role: res.role }, isAuthenticated: true })
  }

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password)
    storeAuth(res)
  }, [])

  const register = useCallback(async (username: string, email: string, password: string) => {
    const res = await authApi.register(username, email, password)
    storeAuth(res)
  }, [])

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem('refreshToken') ?? ''
    await authApi.logout(refreshToken).catch(() => {})
    localStorage.clear()
    setState({ user: null, isAuthenticated: false })
  }, [])

  return (
    <AuthContext.Provider value={{ ...state, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

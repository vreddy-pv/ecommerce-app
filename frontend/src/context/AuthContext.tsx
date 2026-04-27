import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react'
import keycloak from '../keycloak'

interface AuthUser {
  username: string
  role: string
  userId: string
}

interface AuthState {
  user: AuthUser | null
  isAuthenticated: boolean
}

interface AuthContextValue extends AuthState {
  /** Redirect to Keycloak login page (Authorization Code + PKCE). */
  login: () => void
  /** Logout from Keycloak and clear local state. */
  logout: () => void
  /** Returns a valid access token, refreshing if < 30s remaining. */
  getToken: () => Promise<string>
}

const AuthContext = createContext<AuthContextValue | null>(null)

function parseUser(parsed: Record<string, unknown>): AuthUser {
  const roles = (parsed.realm_access as { roles?: string[] })?.roles ?? []
  return {
    username: (parsed.preferred_username as string) ?? (parsed.sub as string),
    role: roles.includes('admin') ? 'ADMIN' : 'USER',
    userId: parsed.sub as string,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(() => {
    // Keycloak is already initialized by main.tsx — read its current auth state
    if (keycloak.authenticated && keycloak.tokenParsed) {
      return {
        isAuthenticated: true,
        user: parseUser(keycloak.tokenParsed as Record<string, unknown>),
      }
    }
    return { isAuthenticated: false, user: null }
  })

  useEffect(() => {
    // Sync if Keycloak authenticated asynchronously after component mount
    if (keycloak.authenticated && keycloak.tokenParsed && !state.isAuthenticated) {
      setState({
        isAuthenticated: true,
        user: parseUser(keycloak.tokenParsed as Record<string, unknown>),
      })
    }

    // Keep user info in sync after auto token refresh
    keycloak.onAuthRefreshSuccess = () => {
      if (keycloak.tokenParsed) {
        setState((prev) => ({
          ...prev,
          user: parseUser(keycloak.tokenParsed as Record<string, unknown>),
        }))
      }
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const login = useCallback(() => {
    keycloak.login()
  }, [])

  const logout = useCallback(() => {
    keycloak.logout({ redirectUri: window.location.origin })
  }, [])

  const getToken = useCallback(async (): Promise<string> => {
    // Refresh if less than 30 seconds until expiry
    await keycloak.updateToken(30)
    return keycloak.token!
  }, [])

  return (
    <AuthContext.Provider value={{ ...state, login, logout, getToken }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

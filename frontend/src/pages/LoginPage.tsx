import { useEffect } from 'react'
import { useAuth } from '../context/AuthContext'

/**
 * LoginPage — immediately redirects to Keycloak's login page.
 *
 * Keycloak handles the full login UI (username/password, Google Sign-In,
 * registration, password reset, MFA) — no custom form needed here.
 *
 * Google Sign-In can be enabled via Keycloak Admin UI:
 *   http://localhost:8180 → Realm: ecommerce → Identity Providers → Add → Google
 */
export default function LoginPage() {
  const { isAuthenticated, login } = useAuth()

  useEffect(() => {
    if (!isAuthenticated) {
      login()  // triggers Keycloak Authorization Code + PKCE redirect
    }
  }, [isAuthenticated, login])

  return (
    <div style={{ textAlign: 'center', padding: 80, color: '#666' }}>
      <p>Redirecting to login…</p>
    </div>
  )
}

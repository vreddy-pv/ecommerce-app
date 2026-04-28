import Keycloak from 'keycloak-js'

/**
 * Singleton Keycloak instance.
 * Initialized in main.tsx before React renders.
 * Import and use this anywhere in the app.
 */
const keycloak = new Keycloak({
  url:      import.meta.env.VITE_KEYCLOAK_URL      ?? 'http://localhost:8180',
  realm:    import.meta.env.VITE_KEYCLOAK_REALM    ?? 'ecommerce',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'ecommerce-frontend',
})

export default keycloak

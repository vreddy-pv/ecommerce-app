import React from 'react'
import ReactDOM from 'react-dom/client'
import keycloak from './keycloak'
import App from './App'

/**
 * Initialize Keycloak BEFORE React renders.
 *
 * onLoad: 'check-sso'  — silent SSO: if the user has an active Keycloak session
 *   the app loads authenticated; otherwise it loads as a guest (no forced redirect).
 *   The LoginPage will trigger keycloak.login() for protected routes.
 *
 * pkceMethod: 'S256'   — required by the ecommerce-frontend client configuration.
 *
 * silentCheckSsoRedirectUri — points to the static file that posts the SSO result
 *   back to the parent window (public/silent-check-sso.html).
 */
keycloak
  .init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
  })
  .then(() => {
    ReactDOM.createRoot(document.getElementById('root')!).render(
      <React.StrictMode>
        <App />
      </React.StrictMode>
    )
  })
  .catch((err) => {
    console.error('Keycloak init failed:', err)
    // Render the app anyway so the user sees an error page rather than a blank screen
    ReactDOM.createRoot(document.getElementById('root')!).render(
      <React.StrictMode>
        <App />
      </React.StrictMode>
    )
  })

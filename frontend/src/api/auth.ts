/**
 * Auth API module — superseded by Keycloak.
 *
 * Authentication is now handled entirely by Keycloak (Authorization Code + PKCE).
 * The keycloak-js adapter in keycloak.ts and AuthContext.tsx replaces all token management.
 *
 * This file is kept as a placeholder. The /api/auth/* endpoints on the backend
 * are no longer called by the frontend — Keycloak's token endpoint is used instead.
 */

// No exports needed — Keycloak handles all auth flows.

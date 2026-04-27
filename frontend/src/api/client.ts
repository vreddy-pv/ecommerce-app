import axios from 'axios'
import keycloak from '../keycloak'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

/**
 * Request interceptor — attach a valid Keycloak Bearer token to every request.
 * updateToken(30) refreshes the token if it expires in less than 30 seconds.
 */
client.interceptors.request.use(async (config) => {
  try {
    await keycloak.updateToken(30)
    if (keycloak.token) {
      config.headers.Authorization = `Bearer ${keycloak.token}`
    }
  } catch {
    // Token refresh failed — redirect to Keycloak login
    keycloak.login()
  }
  return config
})

/**
 * Response interceptor — on 401 force re-login via Keycloak.
 * This handles cases where the gateway rejects an expired token before updateToken fires.
 */
client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      keycloak.login()
    }
    return Promise.reject(error)
  }
)

export default client

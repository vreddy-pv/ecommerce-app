# Keycloak Admin Operations

Perform Keycloak admin tasks: check users, manage roles, test tokens, debug auth issues.

## Usage
```
/keycloak              → show realm summary and all users
/keycloak token        → get and display an admin JWT for API testing
/keycloak mcp-token    → get the MCP service account token and decode it
/keycloak user <name>  → show details for a specific user
/keycloak roles        → list all realm roles and their assignments
```

## Steps based on $ARGUMENTS

### No argument (default — realm summary)
1. Verify Keycloak is reachable: `curl -s http://localhost:8180/realms/ecommerce/.well-known/openid-configuration`
2. Get an admin token for the ecommerce realm
3. List all users: `GET /admin/realms/ecommerce/users`
4. Show a table: username, email, enabled, roles, requiredActions
5. Flag any users with requiredActions (they can't log in until cleared)

### `token` — get admin JWT
1. POST to Keycloak with admin credentials (password grant)
2. Display the raw JWT
3. Decode and pretty-print the payload (sub, preferred_username, realm_access.roles, exp)
4. Show how to use it: `curl -H "Authorization: Bearer <token>" http://localhost:8080/api/orders/admin/summary`

### `mcp-token` — test MCP service account
1. POST client_credentials grant for mcp-server client
2. Decode the JWT payload
3. Verify `realm_access.roles` contains `admin` — warn if not
4. Show the token expiry time

### `user <name>` — user details
1. GET user by username from ecommerce realm
2. Show: id, email, firstName, lastName, enabled, roles, requiredActions, credentials
3. Flag any issues (missing email, UPDATE_PASSWORD required action, etc.)

### `roles` — role assignments
1. List all realm roles
2. For each user, show which roles they have assigned
3. Confirm `service-account-mcp-server` has `admin` role

## Keycloak Admin UI
http://localhost:8180 → admin / admin → select realm: ecommerce

## Notes
- Keycloak uses port **8180** externally (mapped from container port 8080)
- Inside Docker network, services reach Keycloak at `http://keycloak:8080`
- The ecommerce realm config lives at `infrastructure/keycloak/ecommerce-realm.json`
- Changes made via Admin UI are NOT persisted to that JSON file (export manually if needed)

package com.ecommerce.gateway.security;

import com.ecommerce.common.model.UserRole;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Extracts user-identity claims from a Keycloak-issued JWT.
 *
 * Keycloak claim layout (differs from the old custom JWT):
 *   sub               → user UUID (used as X-User-Id)
 *   preferred_username → human-readable username (used as X-User-Name)
 *   realm_access.roles → list of realm role names (used to determine X-User-Role)
 */
@Component
public class KeycloakClaimExtractor {

    public String extractUserId(Jwt jwt) {
        // Keycloak's "sub" is the user UUID (e.g. "550e8400-e29b-41d4-a716-446655440000")
        return jwt.getSubject();
    }

    public String extractUsername(Jwt jwt) {
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred != null ? preferred : jwt.getSubject();
    }

    public UserRole extractRole(Jwt jwt) {
        // Keycloak stores roles as: realm_access: { roles: ["user", "admin"] }
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return UserRole.USER;

        Object roles = realmAccess.get("roles");
        if (roles instanceof List<?> roleList) {
            boolean isAdmin = roleList.stream()
                .map(Object::toString)
                .anyMatch(r -> r.equalsIgnoreCase("admin"));
            return isAdmin ? UserRole.ADMIN : UserRole.USER;
        }
        return UserRole.USER;
    }
}

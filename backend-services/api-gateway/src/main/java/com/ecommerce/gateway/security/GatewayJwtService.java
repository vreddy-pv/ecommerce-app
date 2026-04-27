package com.ecommerce.gateway.security;

import com.ecommerce.common.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PublicKey;

/**
 * Gateway only validates JWTs — it never issues them.
 * Uses the RS256 public key only; private key stays in user-service.
 */
@Slf4j
@Service
public class GatewayJwtService {

    private final PublicKey publicKey;

    public GatewayJwtService(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    public UserRole extractRole(String token) {
        return UserRole.valueOf(getClaims(token).get("role", String.class));
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

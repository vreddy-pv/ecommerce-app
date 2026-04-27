package com.ecommerce.user.service;

import com.ecommerce.common.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();
        // 15-minute access token, 7-day refresh token
        jwtService = new JwtService(keyPair.getPrivate(), keyPair.getPublic(), 900_000L, 604_800_000L);
    }

    @Test
    void generatedAccessTokenIsValid() {
        String token = jwtService.generateAccessToken(1L, "alice", UserRole.USER);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void extractsUsernameFromToken() {
        String token = jwtService.generateAccessToken(1L, "alice", UserRole.USER);
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void extractsUserIdFromToken() {
        String token = jwtService.generateAccessToken(42L, "bob", UserRole.USER);
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void extractsRoleFromToken() {
        String token = jwtService.generateAccessToken(1L, "admin", UserRole.ADMIN);
        assertThat(jwtService.extractRole(token)).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void expiredTokenIsInvalid() {
        // Create a JwtService with 0ms expiry to produce an immediately expired token
        KeyPair keyPair = jwtService.getKeyPair();
        JwtService shortLivedService = new JwtService(keyPair.getPrivate(), keyPair.getPublic(), 0L, 0L);
        String token = shortLivedService.generateAccessToken(1L, "alice", UserRole.USER);
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void tamperedTokenIsInvalid() {
        String token = jwtService.generateAccessToken(1L, "alice", UserRole.USER);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void generatesDifferentTokensOnEachCall() {
        String t1 = jwtService.generateAccessToken(1L, "alice", UserRole.USER);
        String t2 = jwtService.generateAccessToken(1L, "alice", UserRole.USER);
        // Tokens are different because issuedAt timestamp differs
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void refreshTokenIsGenerated() {
        String refreshToken = jwtService.generateRefreshToken();
        assertThat(refreshToken).isNotBlank();
        assertThat(refreshToken).hasSizeGreaterThan(32);
    }

    @Test
    void refreshTokenHashIsDeterministic() {
        String refreshToken = "test-token-value";
        String hash1 = jwtService.hashRefreshToken(refreshToken);
        String hash2 = jwtService.hashRefreshToken(refreshToken);
        assertThat(hash1).isEqualTo(hash2);
    }
}

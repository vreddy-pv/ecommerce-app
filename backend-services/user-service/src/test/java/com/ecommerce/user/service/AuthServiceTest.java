package com.ecommerce.user.service;

import com.ecommerce.common.model.UserRole;
import com.ecommerce.user.domain.RefreshToken;
import com.ecommerce.user.domain.User;
import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.repository.RefreshTokenRepository;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .passwordHash("$2a$hashed")
            .role(UserRole.USER)
            .isActive(true)
            .failedLoginAttempts(0)
            .build();
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @Test
    void registerCreatesUserAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(userRepository.save(any())).thenReturn(existingUser);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access.token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtService.hashRefreshToken("refresh-token")).thenReturn("hashed-refresh");
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access.token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");

        assertThatThrownBy(() -> authService.register(request))
            .hasMessageContaining("email")
            .hasMessageContaining("already");
    }

    @Test
    void registerThrowsWhenUsernameAlreadyExists() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("alice", "new@example.com", "password123");

        assertThatThrownBy(() -> authService.register(request))
            .hasMessageContaining("username")
            .hasMessageContaining("already");
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Test
    void loginWithValidCredentialsReturnsTokens() {
        LoginRequest request = new LoginRequest("alice", "password123");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(1L, "alice", UserRole.USER)).thenReturn("access.token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtService.hashRefreshToken("refresh-token")).thenReturn("hashed-refresh");
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access.token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(existingUser); // resets failed attempts
    }

    @Test
    void loginWithWrongPasswordThrows() {
        LoginRequest request = new LoginRequest("alice", "wrongpass");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongpass", "$2a$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .hasMessageContaining("Invalid credentials");
    }

    @Test
    void loginIncreasesFailedAttemptCount() {
        LoginRequest request = new LoginRequest("alice", "wrongpass");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request));

        assertThat(existingUser.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(existingUser);
    }

    @Test
    void loginLocksAccountAfterFiveFailedAttempts() {
        existingUser.setFailedLoginAttempts(4);
        LoginRequest request = new LoginRequest("alice", "wrongpass");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request));

        assertThat(existingUser.isLocked()).isTrue();
        assertThat(existingUser.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void loginThrowsWhenAccountIsLocked() {
        existingUser.setLocked(true);
        existingUser.setLockedUntil(Instant.now().plusSeconds(600));
        LoginRequest request = new LoginRequest("alice", "password123");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.login(request))
            .hasMessageContaining("locked");
    }

    @Test
    void loginWithUnknownUsernameThrows() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest("unknown", "password123");

        assertThatThrownBy(() -> authService.login(request))
            .hasMessageContaining("Invalid credentials");
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    @Test
    void refreshWithValidTokenReturnsNewAccessToken() {
        RefreshToken storedToken = RefreshToken.builder()
            .id(1L)
            .user(existingUser)
            .tokenHash("hashed-refresh")
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false)
            .build();

        when(jwtService.hashRefreshToken("refresh-token")).thenReturn("hashed-refresh");
        when(refreshTokenRepository.findByTokenHash("hashed-refresh"))
            .thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken(1L, "alice", UserRole.USER)).thenReturn("new.access.token");

        String newAccessToken = authService.refresh("refresh-token");

        assertThat(newAccessToken).isEqualTo("new.access.token");
    }

    @Test
    void refreshWithRevokedTokenThrows() {
        RefreshToken storedToken = RefreshToken.builder()
            .tokenHash("hashed-refresh")
            .revoked(true)
            .build();

        when(jwtService.hashRefreshToken("refresh-token")).thenReturn("hashed-refresh");
        when(refreshTokenRepository.findByTokenHash("hashed-refresh"))
            .thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
            .hasMessageContaining("revoked");
    }

    @Test
    void refreshWithExpiredTokenThrows() {
        RefreshToken storedToken = RefreshToken.builder()
            .tokenHash("hashed-refresh")
            .expiresAt(Instant.now().minusSeconds(60))
            .revoked(false)
            .build();

        when(jwtService.hashRefreshToken("refresh-token")).thenReturn("hashed-refresh");
        when(refreshTokenRepository.findByTokenHash("hashed-refresh"))
            .thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
            .hasMessageContaining("expired");
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    @Test
    void logoutRevokesRefreshToken() {
        RefreshToken storedToken = RefreshToken.builder()
            .tokenHash("hashed-refresh")
            .revoked(false)
            .build();

        when(jwtService.hashRefreshToken("refresh-token")).thenReturn("hashed-refresh");
        when(refreshTokenRepository.findByTokenHash("hashed-refresh"))
            .thenReturn(Optional.of(storedToken));

        authService.logout("refresh-token");

        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(storedToken);
    }
}

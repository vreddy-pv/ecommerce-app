package com.ecommerce.user.service;

import com.ecommerce.common.exception.ServiceException;
import com.ecommerce.common.model.UserRole;
import com.ecommerce.user.domain.RefreshToken;
import com.ecommerce.user.domain.User;
import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.repository.RefreshTokenRepository;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 900; // 15 minutes

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ServiceException("email already in use", "EMAIL_TAKEN", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ServiceException("username already in use", "USERNAME_TAKEN", HttpStatus.CONFLICT);
        }

        User user = User.builder()
            .username(request.username())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(UserRole.USER)
            .build();
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new ServiceException("Invalid credentials", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED));

        if (user.isLocked() && user.getLockedUntil().isAfter(Instant.now())) {
            throw new ServiceException(
                "Account is locked until " + user.getLockedUntil(),
                "ACCOUNT_LOCKED", HttpStatus.UNAUTHORIZED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailedAttempt(user);
            throw new ServiceException("Invalid credentials", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
        user.setLockedUntil(null);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public String refresh(String rawRefreshToken) {
        String hash = jwtService.hashRefreshToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new ServiceException("Refresh token not found", "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked()) {
            throw new ServiceException("Refresh token has been revoked", "REVOKED_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ServiceException("Refresh token has expired", "EXPIRED_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }

        User user = stored.getUser();
        return jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = jwtService.hashRefreshToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private void recordFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLocked(true);
            user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_SECONDS));
            log.warn("Account locked for user '{}' after {} failed attempts", user.getUsername(), attempts);
        }
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String rawRefreshToken = jwtService.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .tokenHash(jwtService.hashRefreshToken(rawRefreshToken))
            .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiryMs()))
            .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, user.getUsername(), user.getRole().name());
    }

    // Purge expired refresh tokens nightly
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Purged expired refresh tokens");
    }
}

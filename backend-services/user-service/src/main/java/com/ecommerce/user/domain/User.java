package com.ecommerce.user.domain;

import com.ecommerce.common.model.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash")          // nullable — Google users have no password
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean isLocked = false;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    // ── Legacy OAuth fields (kept for DB compatibility; auth is now via Keycloak) ──

    /** Auth provider identifier — legacy column, retained for DB compatibility. */
    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private String authProvider = "KEYCLOAK";

    /** Google subject — legacy column, no longer populated. */
    @Column(name = "google_sub", unique = true, length = 100)
    private String googleSub;

    /** Avatar URL — legacy column, retained for future profile features. */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}

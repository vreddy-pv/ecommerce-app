package com.ecommerce.gateway.filter;

import com.ecommerce.common.model.UserRole;
import com.ecommerce.gateway.security.KeycloakClaimExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive JWT authentication filter — validates Keycloak tokens via JWKS.
 *
 * Flow:
 *   1. Public paths bypass JWT check entirely.
 *   2. Bearer token is decoded by NimbusReactiveJwtDecoder (non-blocking, JWKS-cached).
 *   3. Keycloak claims are extracted and forwarded as X-User-* headers.
 *   4. Admin paths reject non-ADMIN tokens with 403.
 *   5. Raw Authorization header is stripped before proxying downstream.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/actuator/",
        "/health"
    );

    /** Paths that require the ADMIN role (regular USER tokens → 403). */
    private static final List<String> ADMIN_PATHS = List.of(
        "/api/orders/admin/",
        "/api/inventory/admin/"
    );

    private final ReactiveJwtDecoder jwtDecoder;
    private final KeycloakClaimExtractor extractor;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        return jwtDecoder.decode(token)
            .flatMap(jwt -> {
                String userId   = extractor.extractUserId(jwt);
                String username = extractor.extractUsername(jwt);
                UserRole role   = extractor.extractRole(jwt);

                // Enforce ADMIN role for admin endpoints
                if (isAdminPath(path) && role != UserRole.ADMIN) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }

                // Forward user context to downstream services via headers
                ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id",   userId)
                    .header("X-User-Name", username)
                    .header("X-User-Role", role.name())
                    // Strip the raw JWT — downstream services trust the injected headers
                    .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
                    .build();

                return chain.filter(exchange.mutate().request(mutated).build());
            })
            .onErrorResume(e -> {
                log.debug("JWT validation failed: {}", e.getMessage());
                return unauthorized(exchange);
            });
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isAdminPath(String path) {
        return ADMIN_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}

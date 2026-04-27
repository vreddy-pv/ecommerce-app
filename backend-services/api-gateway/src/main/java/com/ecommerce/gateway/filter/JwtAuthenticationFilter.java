package com.ecommerce.gateway.filter;

import com.ecommerce.common.model.UserRole;
import com.ecommerce.gateway.security.GatewayJwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/",
        "/actuator/",
        "/health"
    );

    private final GatewayJwtService jwtService;

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
        if (!jwtService.isTokenValid(token)) {
            return unauthorized(exchange);
        }

        Long userId = jwtService.extractUserId(token);
        String username = jwtService.extractUsername(token);
        UserRole role = jwtService.extractRole(token);

        // Mutate request to add user context headers for downstream services
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header("X-User-Id", String.valueOf(userId))
            .header("X-User-Name", username)
            .header("X-User-Role", role.name())
            // Strip the raw JWT — downstream services trust the headers
            .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
            .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}

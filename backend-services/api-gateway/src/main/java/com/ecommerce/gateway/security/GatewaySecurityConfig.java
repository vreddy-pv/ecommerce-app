package com.ecommerce.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway WebFlux security configuration.
 *
 * spring-boot-starter-oauth2-resource-server auto-configures a
 * SecurityWebFilterChain that requires authentication for every request.
 * We disable that default behaviour here because authentication is handled
 * entirely by our custom JwtAuthenticationFilter (WebFilter, @Order(1)).
 *
 * The NimbusReactiveJwtDecoder bean (declared in GatewayJwtConfig) is still
 * used by JwtAuthenticationFilter — we just don't let Spring Security also
 * try to enforce its own resource-server rules on top.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            // Permit everything — JwtAuthenticationFilter handles auth/403/401
            .authorizeExchange(auth -> auth.anyExchange().permitAll())
            .build();
    }
}

package com.ecommerce.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Configures JWT validation via Keycloak's JWKS endpoint.
 *
 * NimbusReactiveJwtDecoder fetches the public keys from Keycloak at runtime
 * and caches them — no static RSA key in environment variables needed.
 */
@Configuration
public class GatewayJwtConfig {

    @Value("${keycloak.jwks-uri}")
    private String jwksUri;

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build();
    }
}

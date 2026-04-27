package com.ecommerce.gateway.filter;

import com.ecommerce.common.model.UserRole;
import com.ecommerce.gateway.security.KeycloakClaimExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {

    @Mock private ReactiveJwtDecoder jwtDecoder;
    @Mock private KeycloakClaimExtractor extractor;
    @Mock private WebFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtDecoder, extractor);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    private Jwt fakeJwt(String sub) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(sub)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(900))
            .claim("realm_access", Map.of("roles", java.util.List.of("user")))
            .build();
    }

    @Test
    void requestWithValidJwtPassesThrough() {
        Jwt jwt = fakeJwt("user-uuid-123");
        when(jwtDecoder.decode("valid.jwt.token")).thenReturn(Mono.just(jwt));
        when(extractor.extractUserId(jwt)).thenReturn("user-uuid-123");
        when(extractor.extractUsername(jwt)).thenReturn("alice");
        when(extractor.extractRole(jwt)).thenReturn(UserRole.USER);

        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/catalog/products")
            .header("Authorization", "Bearer valid.jwt.token")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void requestWithInvalidJwtReturns401() {
        when(jwtDecoder.decode("bad.jwt.token"))
            .thenReturn(Mono.error(new JwtException("invalid token")));

        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/orders")
            .header("Authorization", "Bearer bad.jwt.token")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        verify(chain, never()).filter(any());
    }

    @Test
    void requestWithMissingAuthHeaderReturns401() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/orders")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        verify(chain, never()).filter(any());
        verify(jwtDecoder, never()).decode(anyString());
    }

    @Test
    void actuatorHealthEndpointSkipsJwtValidation() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/actuator/health")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        verify(jwtDecoder, never()).decode(anyString());
        verify(chain).filter(any());
    }

    @Test
    void validJwtSetsDownstreamHeaders() {
        Jwt jwt = fakeJwt("admin-uuid-42");
        when(jwtDecoder.decode("valid.jwt.token")).thenReturn(Mono.just(jwt));
        when(extractor.extractUserId(jwt)).thenReturn("admin-uuid-42");
        when(extractor.extractUsername(jwt)).thenReturn("bob");
        when(extractor.extractRole(jwt)).thenReturn(UserRole.ADMIN);

        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/catalog/products")
            .header("Authorization", "Bearer valid.jwt.token")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex ->
            "admin-uuid-42".equals(ex.getRequest().getHeaders().getFirst("X-User-Id")) &&
            "bob".equals(ex.getRequest().getHeaders().getFirst("X-User-Name")) &&
            "ADMIN".equals(ex.getRequest().getHeaders().getFirst("X-User-Role"))
        ));
    }

    @Test
    void nonAdminTokenOnAdminPathReturns403() {
        Jwt jwt = fakeJwt("user-uuid-99");
        when(jwtDecoder.decode("user.jwt.token")).thenReturn(Mono.just(jwt));
        when(extractor.extractUserId(jwt)).thenReturn("user-uuid-99");
        when(extractor.extractUsername(jwt)).thenReturn("regularuser");
        when(extractor.extractRole(jwt)).thenReturn(UserRole.USER);

        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/orders/admin/summary")
            .header("Authorization", "Bearer user.jwt.token")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        verify(chain, never()).filter(any());
    }
}

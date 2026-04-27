package com.ecommerce.gateway.filter;

import com.ecommerce.common.model.UserRole;
import com.ecommerce.gateway.security.GatewayJwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {

    @Mock private GatewayJwtService jwtService;
    @Mock private WebFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void requestWithValidJwtPassesThrough() {
        when(jwtService.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(jwtService.extractUserId("valid.jwt.token")).thenReturn(1L);
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("alice");
        when(jwtService.extractRole("valid.jwt.token")).thenReturn(UserRole.USER);

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
        when(jwtService.isTokenValid("bad.jwt.token")).thenReturn(false);

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
    }

    @Test
    void publicAuthEndpointsSkipJwtValidation() {
        MockServerHttpRequest request = MockServerHttpRequest
            .post("/api/auth/login")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        verify(jwtService, never()).isTokenValid(any());
        verify(chain).filter(any());
    }

    @Test
    void actuatorHealthEndpointSkipsJwtValidation() {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/actuator/health")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        verify(jwtService, never()).isTokenValid(any());
        verify(chain).filter(any());
    }

    @Test
    void validJwtSetsDownstreamHeaders() {
        when(jwtService.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(jwtService.extractUserId("valid.jwt.token")).thenReturn(42L);
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("bob");
        when(jwtService.extractRole("valid.jwt.token")).thenReturn(UserRole.ADMIN);

        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/catalog/products")
            .header("Authorization", "Bearer valid.jwt.token")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        // Downstream services receive user context headers
        verify(chain).filter(argThat(ex ->
            "42".equals(ex.getRequest().getHeaders().getFirst("X-User-Id")) &&
            "bob".equals(ex.getRequest().getHeaders().getFirst("X-User-Name")) &&
            "ADMIN".equals(ex.getRequest().getHeaders().getFirst("X-User-Role"))
        ));
    }
}

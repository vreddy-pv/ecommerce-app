package com.ecommerce.gateway.filter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Strips X-Internal-API-Key from all inbound requests.
 * Prevents external callers from spoofing internal service identity.
 */
@Component
@Order(-1)
public class InternalKeyStripFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var mutated = exchange.getRequest().mutate()
            .headers(h -> h.remove("X-Internal-API-Key"))
            .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }
}

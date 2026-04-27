package com.ecommerce.aggregator.service;

import com.ecommerce.aggregator.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardAggregatorService {

    private final WebClient.Builder webClientBuilder;

    @Value("${clients.catalog-service:http://catalog-service:8083}")
    private String catalogUrl;

    @Value("${clients.inventory-service:http://inventory-service:8084}")
    private String inventoryUrl;

    @Value("${clients.order-service:http://order-service:8085}")
    private String orderUrl;

    @Value("${clients.user-service:http://user-service:8082}")
    private String userUrl;

    public DashboardResponse aggregate(Long userId) {
        Mono<Object> productsMono  = fetchSilently(catalogUrl,   "/api/catalog/products?page=0&size=6");
        Mono<Object> inventoryMono = fetchSilently(inventoryUrl, "/api/inventory/summary");
        Mono<Object> ordersMono    = fetchSilently(orderUrl,     "/api/orders?userId=" + userId + "&page=0&size=5");
        Mono<Object> userMono      = fetchSilently(userUrl,      "/api/users/" + userId);

        return Mono.zip(productsMono, inventoryMono, ordersMono, userMono)
            .map(tuple -> DashboardResponse.builder()
                .featuredProducts(tuple.getT1())
                .inventoryStatus(tuple.getT2())
                .recentOrders(tuple.getT3())
                .userProfile(tuple.getT4())
                .build())
            .block(Duration.ofSeconds(10));
    }

    private Mono<Object> fetchSilently(String baseUrl, String path) {
        return webClientBuilder.baseUrl(baseUrl).build()
            .get().uri(path)
            .retrieve()
            .bodyToMono(Object.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(ex -> {
                log.warn("Downstream call failed for {}{}: {}", baseUrl, path, ex.getMessage());
                return Mono.just("UNAVAILABLE");
            });
    }
}

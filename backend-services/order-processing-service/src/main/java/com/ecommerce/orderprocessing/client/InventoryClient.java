package com.ecommerce.orderprocessing.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${clients.inventory-service:http://inventory-service:8084}")
    private String inventoryServiceUrl;

    @SneakyThrows
    public void reserve(Long orderId, List<Map<String, Object>> items) {
        String body = objectMapper.writeValueAsString(Map.of("orderId", orderId, "items", items));

        webClientBuilder.baseUrl(inventoryServiceUrl).build()
            .post()
            .uri("/api/inventory/reserve")
            .header("Content-Type", "application/json")
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .block();

        log.info("Reserved inventory for order {}", orderId);
    }
}

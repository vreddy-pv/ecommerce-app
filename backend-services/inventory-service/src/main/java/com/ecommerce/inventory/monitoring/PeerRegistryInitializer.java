package com.ecommerce.inventory.monitoring;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PeerRegistryInitializer {

    private final PeerRegistry peerRegistry;

    @Value("${peers.order-service-url:}")
    private String orderServiceUrl;

    @Value("${peers.order-processing-url:}")
    private String orderProcessingUrl;

    @PostConstruct
    public void init() {
        if (orderServiceUrl != null && !orderServiceUrl.isBlank()) {
            peerRegistry.registerPeer("order-service", orderServiceUrl);
        }
        if (orderProcessingUrl != null && !orderProcessingUrl.isBlank()) {
            peerRegistry.registerPeer("order-processing-service", orderProcessingUrl);
        }
    }
}

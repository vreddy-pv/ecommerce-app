package com.ecommerce.order.monitoring;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PeerRegistryInitializer {

    private final PeerRegistry peerRegistry;

    @Value("${peers.inventory-service:http://inventory-service:8084}")
    private String inventoryServiceUrl;

    @Value("${peers.order-processing-service:http://order-processing-service:8086}")
    private String orderProcessingServiceUrl;

    @PostConstruct
    public void init() {
        peerRegistry.registerPeer("inventory-service", inventoryServiceUrl);
        peerRegistry.registerPeer("order-processing-service", orderProcessingServiceUrl);
    }
}

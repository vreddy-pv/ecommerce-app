package com.ecommerce.orderprocessing.monitoring;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PeerRegistryInitializer {

    private final PeerRegistry peerRegistry;

    @Value("${peers.order-service:http://order-service:8085}")
    private String orderServiceUrl;

    @Value("${peers.inventory-service:http://inventory-service:8084}")
    private String inventoryServiceUrl;

    @PostConstruct
    public void init() {
        peerRegistry.registerPeer("order-service", orderServiceUrl);
        peerRegistry.registerPeer("inventory-service", inventoryServiceUrl);
    }
}

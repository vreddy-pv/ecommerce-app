package com.ecommerce.inventory.monitoring;

import com.ecommerce.common.monitoring.HeartbeatRequest;
import com.ecommerce.common.monitoring.HeartbeatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Component
public class HeartbeatScheduler {

    private final WebClient.Builder webClientBuilder;
    private final PeerRegistry peerRegistry;
    private final String serviceId;

    public HeartbeatScheduler(
            WebClient.Builder webClientBuilder,
            PeerRegistry peerRegistry,
            @Value("${spring.application.name}") String serviceId) {
        this.webClientBuilder = webClientBuilder;
        this.peerRegistry = peerRegistry;
        this.serviceId = serviceId;
    }

    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeats() {
        var request = HeartbeatRequest.builder()
            .serviceId(serviceId)
            .status("UP")
            .build();

        peerRegistry.getPeers().forEach((peerId, peerUrl) -> {
            webClientBuilder.baseUrl(peerUrl).build()
                .post()
                .uri("/api/" + peerId.replace("-service", "") + "/heartbeat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(HeartbeatResponse.class)
                .timeout(Duration.ofSeconds(5))
                .subscribe(
                    resp -> {
                        log.debug("Heartbeat OK from {}", peerId);
                        peerRegistry.recordSuccess(peerId);
                    },
                    err -> {
                        log.warn("Heartbeat MISSED from {}: {}", peerId, err.getMessage());
                        peerRegistry.recordMiss(peerId);
                    }
                );
        });
    }
}

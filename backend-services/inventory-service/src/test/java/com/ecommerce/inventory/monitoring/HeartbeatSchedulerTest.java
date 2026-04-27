package com.ecommerce.inventory.monitoring;

import com.ecommerce.common.monitoring.HeartbeatResponse.PeerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeartbeatSchedulerTest {

    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private PeerRegistry peerRegistry;
    private HeartbeatScheduler scheduler;

    @BeforeEach
    void setUp() {
        peerRegistry = new PeerRegistry();
        peerRegistry.registerPeer("order-service", "http://order-service:8085");
        scheduler = new HeartbeatScheduler(webClientBuilder, peerRegistry, "inventory-service");
    }

    @Test
    void peerStartsHealthy() {
        assertThat(peerRegistry.getStatus("order-service")).isEqualTo(PeerStatus.HEALTHY);
    }

    @Test
    void consecutiveMissesMarkPeerAsSuspect() {
        // Simulate 3 consecutive missed heartbeats
        peerRegistry.recordMiss("order-service");
        peerRegistry.recordMiss("order-service");
        peerRegistry.recordMiss("order-service");

        assertThat(peerRegistry.getStatus("order-service")).isEqualTo(PeerStatus.SUSPECT);
    }

    @Test
    void successfulHeartbeatResetsToHealthy() {
        peerRegistry.recordMiss("order-service");
        peerRegistry.recordMiss("order-service");
        peerRegistry.recordMiss("order-service");
        assertThat(peerRegistry.getStatus("order-service")).isEqualTo(PeerStatus.SUSPECT);

        peerRegistry.recordSuccess("order-service");

        assertThat(peerRegistry.getStatus("order-service")).isEqualTo(PeerStatus.HEALTHY);
        assertThat(peerRegistry.getMissCount("order-service")).isZero();
    }

    @Test
    void twoMissesDoNotYetMarkSuspect() {
        peerRegistry.recordMiss("order-service");
        peerRegistry.recordMiss("order-service");

        assertThat(peerRegistry.getStatus("order-service")).isEqualTo(PeerStatus.HEALTHY);
    }

    @Test
    void receivingHeartbeatReturnsOwnStatus() {
        HeartbeatController controller = new HeartbeatController(peerRegistry, "inventory-service");

        var response = controller.receiveHeartbeat(
            new com.ecommerce.common.monitoring.HeartbeatRequest(
                "order-service", "http://order-service:8085", null, "UP"));

        assertThat(response.getServiceId()).isEqualTo("inventory-service");
        assertThat(response.getStatus()).isEqualTo(PeerStatus.HEALTHY);
    }

    @Test
    void missCountResetsAfterSuccess() {
        peerRegistry.recordMiss("order-service");
        assertThat(peerRegistry.getMissCount("order-service")).isEqualTo(1);

        peerRegistry.recordSuccess("order-service");
        assertThat(peerRegistry.getMissCount("order-service")).isZero();
    }

    @Test
    void unknownPeerReturnsNull() {
        assertThat(peerRegistry.getStatus("unknown-service")).isNull();
    }
}

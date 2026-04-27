package com.ecommerce.order.monitoring;

import com.ecommerce.common.monitoring.HeartbeatResponse.PeerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HeartbeatSchedulerTest {

    @Mock private WebClient.Builder webClientBuilder;

    private PeerRegistry peerRegistry;
    private HeartbeatScheduler scheduler;

    @BeforeEach
    void setUp() {
        peerRegistry = new PeerRegistry();
        peerRegistry.registerPeer("inventory-service", "http://inventory-service:8084");
        peerRegistry.registerPeer("order-processing-service", "http://order-processing-service:8086");
        scheduler = new HeartbeatScheduler(webClientBuilder, peerRegistry, "order-service");
    }

    @Test
    void peersStartHealthy() {
        assertThat(peerRegistry.getStatus("inventory-service")).isEqualTo(PeerStatus.HEALTHY);
        assertThat(peerRegistry.getStatus("order-processing-service")).isEqualTo(PeerStatus.HEALTHY);
    }

    @Test
    void threeConsecutiveMissesMarkPeerAsSuspect() {
        peerRegistry.recordMiss("inventory-service");
        peerRegistry.recordMiss("inventory-service");
        peerRegistry.recordMiss("inventory-service");

        assertThat(peerRegistry.getStatus("inventory-service")).isEqualTo(PeerStatus.SUSPECT);
    }

    @Test
    void twoMissesDoNotYetMarkSuspect() {
        peerRegistry.recordMiss("inventory-service");
        peerRegistry.recordMiss("inventory-service");

        assertThat(peerRegistry.getStatus("inventory-service")).isEqualTo(PeerStatus.HEALTHY);
    }

    @Test
    void successAfterSuspectResetsToHealthy() {
        peerRegistry.recordMiss("inventory-service");
        peerRegistry.recordMiss("inventory-service");
        peerRegistry.recordMiss("inventory-service");
        assertThat(peerRegistry.getStatus("inventory-service")).isEqualTo(PeerStatus.SUSPECT);

        peerRegistry.recordSuccess("inventory-service");

        assertThat(peerRegistry.getStatus("inventory-service")).isEqualTo(PeerStatus.HEALTHY);
        assertThat(peerRegistry.getMissCount("inventory-service")).isZero();
    }

    @Test
    void receivingHeartbeatReturnsOwnStatus() {
        HeartbeatController controller = new HeartbeatController(peerRegistry, "order-service");

        var resp = controller.receiveHeartbeat(
            new com.ecommerce.common.monitoring.HeartbeatRequest(
                "inventory-service", "http://inventory-service:8084", null, "UP"));

        assertThat(resp.getServiceId()).isEqualTo("order-service");
        assertThat(resp.getStatus()).isEqualTo(PeerStatus.HEALTHY);
    }

    @Test
    void unknownPeerReturnsNull() {
        assertThat(peerRegistry.getStatus("unknown-service")).isNull();
    }

    @Test
    void missCountResetsAfterSuccess() {
        peerRegistry.recordMiss("order-processing-service");
        assertThat(peerRegistry.getMissCount("order-processing-service")).isEqualTo(1);

        peerRegistry.recordSuccess("order-processing-service");
        assertThat(peerRegistry.getMissCount("order-processing-service")).isZero();
    }
}

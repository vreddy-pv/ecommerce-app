package com.ecommerce.orderprocessing.monitoring;

import com.ecommerce.common.monitoring.HeartbeatRequest;
import com.ecommerce.common.monitoring.HeartbeatResponse;
import com.ecommerce.common.monitoring.HeartbeatResponse.PeerStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order-processing")
public class HeartbeatController {

    private final PeerRegistry peerRegistry;
    private final String serviceId;

    public HeartbeatController(PeerRegistry peerRegistry, @Value("${spring.application.name}") String serviceId) {
        this.peerRegistry = peerRegistry;
        this.serviceId = serviceId;
    }

    @PostMapping("/heartbeat")
    public HeartbeatResponse receiveHeartbeat(@RequestBody HeartbeatRequest request) {
        if (request.getServiceId() != null) {
            peerRegistry.recordSuccess(request.getServiceId());
        }
        return HeartbeatResponse.builder()
            .serviceId(serviceId)
            .status(PeerStatus.HEALTHY)
            .build();
    }
}

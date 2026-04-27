package com.ecommerce.common.monitoring;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class HeartbeatResponse {
    private final String serviceId;
    private final PeerStatus status;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    public enum PeerStatus {
        HEALTHY,
        SUSPECT,
        DOWN
    }
}

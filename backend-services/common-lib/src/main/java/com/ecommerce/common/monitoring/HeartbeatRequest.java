package com.ecommerce.common.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class HeartbeatRequest {
    private final String serviceId;
    private final String serviceUrl;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Builder.Default
    private final String status = "UP";
}

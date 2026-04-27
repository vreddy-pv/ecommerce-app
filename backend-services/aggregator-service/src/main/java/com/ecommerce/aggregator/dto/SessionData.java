package com.ecommerce.aggregator.dto;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Builder
public class SessionData implements Serializable {
    private final Long userId;
    private final String username;
    private final String role;
    private final Instant lastLogin;
}

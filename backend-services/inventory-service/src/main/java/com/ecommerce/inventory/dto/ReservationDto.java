package com.ecommerce.inventory.dto;

import com.ecommerce.inventory.domain.ReservationStatus;

import java.time.Instant;

public record ReservationDto(
    Long id,
    Long orderId,
    Long productId,
    int reservedQty,
    ReservationStatus status,
    Instant expiresAt
) {}

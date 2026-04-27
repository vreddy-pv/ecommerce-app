package com.ecommerce.inventory.dto;

public record LowInventoryAlertDto(
    Long productId,
    int quantity,
    int reservedQuantity,
    int available,
    int reorderThreshold
) {}

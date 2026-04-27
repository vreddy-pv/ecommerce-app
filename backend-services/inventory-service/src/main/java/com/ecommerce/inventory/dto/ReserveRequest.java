package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReserveRequest(
    @NotNull Long orderId,
    @NotNull List<LineItem> items
) {
    public record LineItem(@NotNull Long productId, @Min(1) int quantity) {}
}

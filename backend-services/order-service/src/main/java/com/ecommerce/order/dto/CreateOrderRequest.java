package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
    @NotNull Long userId,
    @NotEmpty @Valid List<LineItem> items
) {
    public record LineItem(
        @NotNull Long productId,
        @Positive int quantity,
        @NotNull BigDecimal unitPrice
    ) {}
}

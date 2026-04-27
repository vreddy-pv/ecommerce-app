package com.ecommerce.order.dto;

import com.ecommerce.order.domain.OrderItem;

import java.math.BigDecimal;

public record OrderItemDto(
    Long id,
    Long productId,
    int quantity,
    BigDecimal unitPrice
) {
    public static OrderItemDto from(OrderItem item) {
        return new OrderItemDto(item.getId(), item.getProductId(), item.getQuantity(), item.getUnitPrice());
    }
}

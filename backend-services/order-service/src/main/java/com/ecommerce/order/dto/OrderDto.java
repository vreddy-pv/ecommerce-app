package com.ecommerce.order.dto;

import com.ecommerce.common.model.OrderStatus;
import com.ecommerce.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
    Long id,
    String userId,
    OrderStatus status,
    BigDecimal totalAmount,
    Instant createdAt,
    Instant updatedAt,
    List<OrderItemDto> items
) {
    public static OrderDto from(Order order) {
        return new OrderDto(
            order.getId(),
            order.getUserId(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getItems().stream().map(OrderItemDto::from).toList()
        );
    }
}

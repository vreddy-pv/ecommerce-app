package com.ecommerce.order.service;

import com.ecommerce.common.model.OrderStatus;
import com.ecommerce.order.config.RabbitMqConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(String payload) {
        updateStatus(payload, OrderStatus.CONFIRMED);
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_FAILED_QUEUE)
    public void onOrderFailed(String payload) {
        updateStatus(payload, OrderStatus.CANCELLED);
    }

    private void updateStatus(String payload, OrderStatus next) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long orderId = node.get("orderId").asLong();
            orderService.updateStatus(orderId, next);
            log.info("Order {} transitioned to {}", orderId, next);
        } catch (Exception ex) {
            log.error("Failed to process status update: {}", ex.getMessage());
        }
    }
}

package com.ecommerce.order.service;

import com.ecommerce.common.exception.InvalidOrderStateException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.model.OrderStatus;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OutboxEvent;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepo;
    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public OrderDto getOrder(Long id) {
        return OrderDto.from(findById(id));
    }

    public List<OrderDto> getOrdersByUser(Long userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(OrderDto::from).toList();
    }

    @Transactional
    public OrderDto createOrder(Long userId, String idempotencyKey, CreateOrderRequest req) {
        return orderRepo.findByIdempotencyKey(idempotencyKey)
            .map(OrderDto::from)
            .orElseGet(() -> {
                Order order = buildOrder(userId, idempotencyKey, req);
                order = orderRepo.save(order);
                outboxRepo.save(outboxEvent(order, "order.created"));
                return OrderDto.from(order);
            });
    }

    @Transactional
    public OrderDto cancelOrder(Long id) {
        Order order = findById(id);
        order.transitionTo(OrderStatus.CANCELLED);
        orderRepo.save(order);
        outboxRepo.save(outboxEvent(order, "order.cancelled"));
        return OrderDto.from(order);
    }

    @Transactional
    public void updateStatus(Long id, OrderStatus next) {
        Order order = findById(id);
        order.transitionTo(next);
        orderRepo.save(order);
    }

    private Order buildOrder(Long userId, String idempotencyKey, CreateOrderRequest req) {
        BigDecimal total = req.items().stream()
            .map(li -> li.unitPrice().multiply(BigDecimal.valueOf(li.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
            .userId(userId)
            .idempotencyKey(idempotencyKey)
            .status(OrderStatus.PENDING)
            .totalAmount(total)
            .build();

        List<OrderItem> items = req.items().stream()
            .map(li -> OrderItem.builder()
                .order(order)
                .productId(li.productId())
                .quantity(li.quantity())
                .unitPrice(li.unitPrice())
                .build())
            .toList();
        order.getItems().addAll(items);
        return order;
    }

    @SneakyThrows
    private OutboxEvent outboxEvent(Order order, String eventType) {
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("orderId", order.getId());
        payloadMap.put("userId", order.getUserId());
        payloadMap.put("status", order.getStatus());
        payloadMap.put("totalAmount", order.getTotalAmount());
        String payload = objectMapper.writeValueAsString(payloadMap);
        return OutboxEvent.builder()
            .aggregateId(order.getId())
            .eventType(eventType)
            .payload(payload)
            .build();
    }

    private Order findById(Long id) {
        return orderRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}

package com.ecommerce.order.service;

import com.ecommerce.common.exception.InvalidOrderStateException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.model.OrderStatus;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OutboxEvent;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.dto.OrderSummaryDto;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    public List<OrderDto> getOrdersByUser(String userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(OrderDto::from).toList();
    }

    @Transactional
    public OrderDto createOrder(String userId, String idempotencyKey, CreateOrderRequest req) {
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

    private Order buildOrder(String userId, String idempotencyKey, CreateOrderRequest req) {
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

    // ── Admin ─────────────────────────────────────────────────────────────────

    public OrderSummaryDto getOrderSummary(String period) {
        Instant since = switch (period.toLowerCase()) {
            case "today" -> Instant.now().truncatedTo(ChronoUnit.DAYS);
            case "30d"   -> Instant.now().minus(30, ChronoUnit.DAYS);
            default      -> Instant.now().minus(7, ChronoUnit.DAYS);   // "7d"
        };

        List<Object[]> rows = orderRepo.getSummaryByStatus(since);
        long totalOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, Long> byStatus = new LinkedHashMap<>();

        for (Object[] row : rows) {
            OrderStatus status = (OrderStatus) row[0];
            long count  = ((Number) row[1]).longValue();
            BigDecimal sum = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            totalOrders += count;
            totalRevenue = totalRevenue.add(sum);
            byStatus.put(status.name(), count);
        }
        // ensure all statuses are present (even if 0)
        Arrays.stream(OrderStatus.values())
              .filter(s -> !byStatus.containsKey(s.name()))
              .forEach(s -> byStatus.put(s.name(), 0L));

        return new OrderSummaryDto(period, totalOrders, totalRevenue, byStatus);
    }

    public List<OrderDto> searchOrders(String q) {
        if (q == null || q.isBlank()) {
            return orderRepo.findTop20ByOrderByCreatedAtDesc()
                .stream().map(OrderDto::from).toList();
        }
        // Try matching a status name
        for (OrderStatus s : OrderStatus.values()) {
            if (s.name().equalsIgnoreCase(q.trim())) {
                return orderRepo.findByStatusOrderByCreatedAtDesc(s)
                    .stream().map(OrderDto::from).toList();
            }
        }
        // Try matching a numeric ID
        try {
            Long id = Long.parseLong(q.trim());
            return orderRepo.findById(id).map(OrderDto::from)
                .map(List::of).orElse(List.of());
        } catch (NumberFormatException ignored) {
            // fall through
        }
        // Default: recent 20
        return orderRepo.findTop20ByOrderByCreatedAtDesc()
            .stream().map(OrderDto::from).toList();
    }

    private Order findById(Long id) {
        return orderRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}

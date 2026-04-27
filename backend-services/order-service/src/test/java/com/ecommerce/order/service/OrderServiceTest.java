package com.ecommerce.order.service;

import com.ecommerce.common.exception.InvalidOrderStateException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.model.OrderStatus;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OutboxEvent;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepo;
    @Mock private OutboxEventRepository outboxRepo;
    @InjectMocks private OrderService orderService;

    // Keycloak UUIDs (sub claim) used as userId
    private static final String USER_UUID   = "550e8400-e29b-41d4-a716-446655440042";
    private static final String USER_UUID_1 = "550e8400-e29b-41d4-a716-446655440001";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepo, outboxRepo, objectMapper);
    }

    private CreateOrderRequest sampleRequest() {
        return new CreateOrderRequest(
            List.of(new CreateOrderRequest.LineItem(100L, 2, new BigDecimal("29.99")))
        );
    }

    // ── Create Order ──────────────────────────────────────────────────────────

    @Test
    void createOrderPersistsOrderAndOutboxEvent() {
        when(orderRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderRepo.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o = Order.builder()
                .id(1L).userId(o.getUserId())
                .idempotencyKey(o.getIdempotencyKey())
                .status(o.getStatus())
                .totalAmount(o.getTotalAmount())
                .build();
            return o;
        });
        when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = orderService.createOrder(USER_UUID, "key-001", sampleRequest());

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.userId()).isEqualTo(USER_UUID);
        assertThat(result.totalAmount()).isEqualByComparingTo("59.98");

        verify(outboxRepo).save(argThat(e -> "order.created".equals(e.getEventType())));
    }

    @Test
    void createOrderIsIdempotentForDuplicateKey() {
        Order existing = Order.builder()
            .id(5L).userId(USER_UUID).idempotencyKey("key-001")
            .status(OrderStatus.PENDING).totalAmount(new BigDecimal("59.98"))
            .build();
        when(orderRepo.findByIdempotencyKey("key-001")).thenReturn(Optional.of(existing));

        OrderDto result = orderService.createOrder(USER_UUID, "key-001", sampleRequest());

        assertThat(result.id()).isEqualTo(5L);
        verify(orderRepo, never()).save(any());
        verify(outboxRepo, never()).save(any());
    }

    @Test
    void createOrderComputesTotalFromLineItems() {
        when(orderRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new CreateOrderRequest(List.of(
            new CreateOrderRequest.LineItem(100L, 3, new BigDecimal("10.00")),
            new CreateOrderRequest.LineItem(200L, 1, new BigDecimal("25.00"))
        ));

        OrderDto result = orderService.createOrder(USER_UUID_1, "key-002", req);

        assertThat(result.totalAmount()).isEqualByComparingTo("55.00");
    }

    // ── Cancel Order ──────────────────────────────────────────────────────────

    @Test
    void cancelPendingOrderSucceeds() {
        Order order = Order.builder()
            .id(1L).userId(USER_UUID_1).idempotencyKey("k")
            .status(OrderStatus.PENDING).totalAmount(BigDecimal.TEN)
            .build();
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = orderService.cancelOrder(1L);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(outboxRepo).save(argThat(e -> "order.cancelled".equals(e.getEventType())));
    }

    @Test
    void cancelShippedOrderThrows() {
        Order order = Order.builder()
            .id(1L).userId(USER_UUID_1).idempotencyKey("k")
            .status(OrderStatus.SHIPPED).totalAmount(BigDecimal.TEN)
            .build();
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
            .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void cancelDeliveredOrderThrows() {
        Order order = Order.builder()
            .id(2L).userId(USER_UUID_1).idempotencyKey("k2")
            .status(OrderStatus.DELIVERED).totalAmount(BigDecimal.TEN)
            .build();
        when(orderRepo.findById(2L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(2L))
            .isInstanceOf(InvalidOrderStateException.class);
    }

    // ── State Machine ─────────────────────────────────────────────────────────

    @Test
    void pendingCanTransitionToConfirmed() {
        Order order = Order.builder()
            .id(1L).userId(USER_UUID_1).idempotencyKey("k")
            .status(OrderStatus.PENDING).totalAmount(BigDecimal.TEN)
            .build();
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(1L, OrderStatus.CONFIRMED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void deliveredCannotTransitionBackToPending() {
        Order order = Order.builder()
            .id(1L).userId(USER_UUID_1).idempotencyKey("k")
            .status(OrderStatus.DELIVERED).totalAmount(BigDecimal.TEN)
            .build();
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.PENDING))
            .isInstanceOf(InvalidOrderStateException.class);
    }

    // ── Get Order ─────────────────────────────────────────────────────────────

    @Test
    void getOrderThrowsWhenNotFound() {
        when(orderRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void outboxEventContainsOrderId() {
        when(orderRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(orderRepo.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return Order.builder()
                .id(99L).userId(o.getUserId())
                .idempotencyKey(o.getIdempotencyKey())
                .status(o.getStatus()).totalAmount(o.getTotalAmount())
                .build();
        });
        when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(USER_UUID, "key-x", sampleRequest());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("order.created");
        assertThat(captor.getValue().getPayload()).contains("99");
    }
}

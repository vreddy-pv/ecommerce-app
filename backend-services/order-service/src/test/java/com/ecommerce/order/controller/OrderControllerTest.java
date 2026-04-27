package com.ecommerce.order.controller;

import com.ecommerce.common.exception.InvalidOrderStateException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.model.OrderStatus;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({com.ecommerce.order.config.SecurityConfig.class,
         com.ecommerce.common.exception.GlobalExceptionHandler.class})
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private OrderService orderService;

    private static final String USER_UUID = "550e8400-e29b-41d4-a716-446655440042";

    private OrderDto sampleOrderDto() {
        return new OrderDto(1L, USER_UUID, OrderStatus.PENDING, new BigDecimal("59.98"),
            Instant.now(), Instant.now(), List.of());
    }

    @Test
    void getOrderReturns200() throws Exception {
        when(orderService.getOrder(1L)).thenReturn(sampleOrderDto());

        mockMvc.perform(get("/api/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getOrderReturns404WhenNotFound() throws Exception {
        when(orderService.getOrder(999L)).thenThrow(new ResourceNotFoundException("Order", 999L));

        mockMvc.perform(get("/api/orders/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createOrderReturns202OnSuccess() throws Exception {
        when(orderService.createOrder(anyString(), any(), any())).thenReturn(sampleOrderDto());

        var req = new CreateOrderRequest(
            List.of(new CreateOrderRequest.LineItem(100L, 2, new BigDecimal("29.99"))));

        mockMvc.perform(post("/api/orders")
                .header("Idempotency-Key", "key-001")
                .header("X-User-Id", "42")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createOrderReturns400WhenItemsEmpty() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header("Idempotency-Key", "key-002")
                .header("X-User-Id", "42")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items":[]}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createOrderReturns400WhenUserIdHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header("Idempotency-Key", "key-003")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items":[{"productId":1,"quantity":1,"unitPrice":9.99}]}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void cancelOrderReturns200() throws Exception {
        OrderDto cancelled = new OrderDto(1L, USER_UUID, OrderStatus.CANCELLED, new BigDecimal("59.98"),
            Instant.now(), Instant.now(), List.of());
        when(orderService.cancelOrder(1L)).thenReturn(cancelled);

        mockMvc.perform(put("/api/orders/1/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void cancelShippedOrderReturns409() throws Exception {
        when(orderService.cancelOrder(1L))
            .thenThrow(new InvalidOrderStateException(OrderStatus.SHIPPED, OrderStatus.CANCELLED));

        mockMvc.perform(put("/api/orders/1/cancel"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("INVALID_ORDER_STATE_TRANSITION"));
    }

    @Test
    void getUserOrdersReturns200() throws Exception {
        when(orderService.getOrdersByUser(USER_UUID)).thenReturn(List.of(sampleOrderDto()));

        mockMvc.perform(get("/api/orders").header("X-User-Id", USER_UUID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].userId").value(USER_UUID));
    }
}

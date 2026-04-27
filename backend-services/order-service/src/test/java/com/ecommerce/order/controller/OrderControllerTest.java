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

    private OrderDto sampleOrderDto() {
        return new OrderDto(1L, 42L, OrderStatus.PENDING, new BigDecimal("59.98"),
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
        when(orderService.createOrder(any(), any())).thenReturn(sampleOrderDto());

        var req = new CreateOrderRequest(42L,
            List.of(new CreateOrderRequest.LineItem(100L, 2, new BigDecimal("29.99"))));

        mockMvc.perform(post("/api/orders")
                .header("Idempotency-Key", "key-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createOrderReturns400WhenItemsEmpty() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header("Idempotency-Key", "key-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":1,"items":[]}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createOrderReturns400WhenUserIdMissing() throws Exception {
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
        OrderDto cancelled = new OrderDto(1L, 42L, OrderStatus.CANCELLED, new BigDecimal("59.98"),
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
        when(orderService.getOrdersByUser(42L)).thenReturn(List.of(sampleOrderDto()));

        mockMvc.perform(get("/api/orders").param("userId", "42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].userId").value(42));
    }
}

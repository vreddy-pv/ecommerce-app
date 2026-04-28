package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.dto.OrderSummaryDto;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrder(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDto>>> getUserOrders(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersByUser(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid CreateOrderRequest req) {
        OrderDto order = orderService.createOrder(userId, idempotencyKey, req);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.ok(order));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.cancelOrder(id)));
    }

    // ── Admin / MCP endpoints (no user context required) ─────────────────────

    @GetMapping("/admin/summary")
    public ResponseEntity<ApiResponse<OrderSummaryDto>> getOrderSummary(
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderSummary(period)));
    }

    @GetMapping("/admin/search")
    public ResponseEntity<ApiResponse<List<OrderDto>>> searchOrders(
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.searchOrders(q)));
    }
}

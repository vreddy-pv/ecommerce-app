package com.ecommerce.inventory.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.inventory.dto.LowInventoryAlertDto;
import com.ecommerce.inventory.dto.ReserveRequest;
import com.ecommerce.inventory.dto.StockDto;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}/stock")
    public ResponseEntity<ApiResponse<StockDto>> getStock(@PathVariable Long productId) {
        StockDto stock = inventoryService.getStock(productId);
        return ResponseEntity.ok(ApiResponse.ok(stock));
    }

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(@Valid @RequestBody ReserveRequest request) {
        inventoryService.reserve(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PutMapping("/release/{orderId}")
    public ResponseEntity<Void> release(@PathVariable Long orderId) {
        inventoryService.release(orderId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/confirm/{orderId}")
    public ResponseEntity<Void> confirm(@PathVariable Long orderId) {
        inventoryService.confirm(orderId);
        return ResponseEntity.noContent().build();
    }

    // ── Admin / MCP endpoint ──────────────────────────────────────────────────

    @GetMapping("/admin/alerts")
    public ResponseEntity<ApiResponse<List<LowInventoryAlertDto>>> getLowInventoryAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowInventoryAlerts()));
    }
}

package com.ecommerce.inventory.dto;

public record StockDto(Long productId, int quantity, int reserved, int available, boolean inStock) {}

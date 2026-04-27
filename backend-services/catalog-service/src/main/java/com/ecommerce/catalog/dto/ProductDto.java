package com.ecommerce.catalog.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

// Serializable required for Redis cache serialization
public record ProductDto(
    Long id,
    String sku,
    String name,
    String description,
    BigDecimal price,
    Long categoryId,
    String categoryName,
    List<String> imageUrls,
    boolean active
) implements Serializable {}

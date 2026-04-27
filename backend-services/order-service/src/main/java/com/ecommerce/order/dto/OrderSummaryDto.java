package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.util.Map;

public record OrderSummaryDto(
    String period,
    long totalOrders,
    BigDecimal totalRevenue,
    Map<String, Long> ordersByStatus
) {}

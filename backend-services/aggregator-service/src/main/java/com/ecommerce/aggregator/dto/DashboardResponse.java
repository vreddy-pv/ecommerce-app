package com.ecommerce.aggregator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {
    private final Object featuredProducts;
    private final Object inventoryStatus;
    private final Object recentOrders;
    private final Object userProfile;
    private final String catalogStatus;
    private final String inventoryServiceStatus;
    private final String orderServiceStatus;
}

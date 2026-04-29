package com.ecommerce.aggregator.controller;

import com.ecommerce.aggregator.dto.ProductViewEventRequest;
import com.ecommerce.aggregator.dto.SearchEventRequest;
import com.ecommerce.aggregator.service.AnalyticsEventService;
import com.ecommerce.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsEventService analyticsEventService;

    /**
     * Log a search event.
     * Called by frontend when user performs a search.
     */
    @PostMapping("/search-event")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<String> logSearchEvent(
            @Valid @RequestBody SearchEventRequest request,
            @RequestHeader("X-User-Id") String userId) {

        analyticsEventService.publishSearchEvent(userId, request);
        return ApiResponse.ok("Event logged");
    }

    /**
     * Log a product view event.
     * Called by frontend when user views a product for a measurable duration.
     */
    @PostMapping("/product-view-event")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<String> logProductViewEvent(
            @Valid @RequestBody ProductViewEventRequest request,
            @RequestHeader("X-User-Id") String userId) {

        analyticsEventService.publishProductViewEvent(userId, request);
        return ApiResponse.ok("Event logged");
    }
}

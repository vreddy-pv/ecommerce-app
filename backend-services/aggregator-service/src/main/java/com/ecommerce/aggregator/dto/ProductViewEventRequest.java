package com.ecommerce.aggregator.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductViewEventRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Session ID is required")
    private String sessionId;

    @NotNull(message = "Duration in seconds is required")
    private Integer durationSeconds;

    @Builder.Default
    private String source = "search";  // 'search', 'category_browse', 'recommendation', 'direct'
}

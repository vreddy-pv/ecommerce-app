package com.ecommerce.aggregator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchEventRequest {

    @NotBlank(message = "Search query cannot be blank")
    private String searchQuery;

    @NotNull(message = "Result count is required")
    private Integer resultCount;

    private Long clickedProductId;  // Optional: populated when user clicks a result
}

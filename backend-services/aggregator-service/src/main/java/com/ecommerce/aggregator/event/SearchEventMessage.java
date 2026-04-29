package com.ecommerce.aggregator.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchEventMessage {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("searchQuery")
    private String searchQuery;

    @JsonProperty("resultCount")
    private Integer resultCount;

    @JsonProperty("clickedProductId")
    private Long clickedProductId;

    @JsonProperty("timestamp")
    private Instant timestamp;
}

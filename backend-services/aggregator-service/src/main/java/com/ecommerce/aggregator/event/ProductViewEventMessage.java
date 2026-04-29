package com.ecommerce.aggregator.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductViewEventMessage {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("productId")
    private Long productId;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("durationSeconds")
    private Integer durationSeconds;

    @JsonProperty("source")
    private String source;

    @JsonProperty("timestamp")
    private Instant timestamp;
}

package com.ecommerce.analytics.service;

import com.ecommerce.analytics.domain.ProductViewEvent;
import com.ecommerce.analytics.domain.RecommendationFeedbackEvent;
import com.ecommerce.analytics.domain.UserSearchEvent;
import com.ecommerce.analytics.repository.ProductViewEventRepository;
import com.ecommerce.analytics.repository.RecommendationFeedbackEventRepository;
import com.ecommerce.analytics.repository.UserSearchEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsEventConsumer {

    private final UserSearchEventRepository searchEventRepository;
    private final ProductViewEventRepository viewEventRepository;
    private final RecommendationFeedbackEventRepository feedbackEventRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "user-activity.search.queue")
    public void onSearchEvent(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            Instant timestamp;
            if (event.get("timestamp").isNumber()) {
                timestamp = Instant.ofEpochMilli(event.get("timestamp").asLong());
            } else {
                timestamp = Instant.parse(event.get("timestamp").asText());
            }

            UserSearchEvent searchEvent = UserSearchEvent.builder()
                .userId(event.get("userId").asText())
                .searchQuery(event.get("searchQuery").asText())
                .resultCount(event.get("resultCount").asInt())
                .clickedProductId(event.has("clickedProductId") && !event.get("clickedProductId").isNull()
                    ? event.get("clickedProductId").asLong()
                    : null)
                .createdAt(timestamp)
                .build();

            searchEventRepository.save(searchEvent);
            log.info("Saved search event: userId={}, query={}", searchEvent.getUserId(), searchEvent.getSearchQuery());
        } catch (Exception e) {
            log.error("Failed to process search event: {}", eventPayload, e);
        }
    }

    @RabbitListener(queues = "user-activity.view.queue")
    public void onProductViewEvent(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            Instant timestamp;
            if (event.get("timestamp").isNumber()) {
                timestamp = Instant.ofEpochMilli(event.get("timestamp").asLong());
            } else {
                timestamp = Instant.parse(event.get("timestamp").asText());
            }

            ProductViewEvent viewEvent = ProductViewEvent.builder()
                .userId(event.has("userId") && !event.get("userId").isNull() ? event.get("userId").asText() : null)
                .productId(event.get("productId").asLong())
                .sessionId(event.get("sessionId").asText())
                .durationSeconds(event.get("durationSeconds").asInt())
                .source(event.get("source").asText())
                .createdAt(timestamp)
                .build();

            viewEventRepository.save(viewEvent);
            log.info("Saved product view event: productId={}, duration={}s", viewEvent.getProductId(), viewEvent.getDurationSeconds());
        } catch (Exception e) {
            log.error("Failed to process product view event: {}", eventPayload, e);
        }
    }

    @RabbitListener(queues = "user-activity.feedback.queue")
    public void onRecommendationFeedback(String eventPayload) {
        try {
            JsonNode event = objectMapper.readTree(eventPayload);
            Instant timestamp;
            if (event.get("timestamp").isNumber()) {
                timestamp = Instant.ofEpochMilli(event.get("timestamp").asLong());
            } else {
                timestamp = Instant.parse(event.get("timestamp").asText());
            }

            RecommendationFeedbackEvent feedbackEvent = RecommendationFeedbackEvent.builder()
                .userId(event.get("userId").asText())
                .productId(event.get("productId").asLong())
                .recommendationId(event.get("recommendationId").asText())
                .action(event.get("action").asText())
                .orderId(event.has("orderId") && !event.get("orderId").isNull()
                    ? event.get("orderId").asLong()
                    : null)
                .createdAt(timestamp)
                .build();

            feedbackEventRepository.save(feedbackEvent);
            log.info("Saved recommendation feedback: userId={}, productId={}, action={}",
                feedbackEvent.getUserId(), feedbackEvent.getProductId(), feedbackEvent.getAction());
        } catch (Exception e) {
            log.error("Failed to process recommendation feedback event: {}", eventPayload, e);
        }
    }
}

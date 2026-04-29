package com.ecommerce.aggregator.service;

import com.ecommerce.aggregator.config.AnalyticsRabbitConfig;
import com.ecommerce.aggregator.dto.ProductViewEventRequest;
import com.ecommerce.aggregator.dto.SearchEventRequest;
import com.ecommerce.aggregator.event.ProductViewEventMessage;
import com.ecommerce.aggregator.event.SearchEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "analytics.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AnalyticsEventService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a search event to RabbitMQ.
     * Called when user performs a search.
     */
    public void publishSearchEvent(String userId, SearchEventRequest request) {
        SearchEventMessage message = SearchEventMessage.builder()
            .eventId(UUID.randomUUID().toString())
            .userId(userId)
            .searchQuery(request.getSearchQuery())
            .resultCount(request.getResultCount())
            .clickedProductId(request.getClickedProductId())
            .timestamp(Instant.now())
            .build();

        try {
            rabbitTemplate.convertAndSend(
                AnalyticsRabbitConfig.USER_ACTIVITY_EXCHANGE,
                AnalyticsRabbitConfig.SEARCH_ROUTING_KEY,
                message
            );
            log.debug("Published search event: userId={}, query={}", userId, request.getSearchQuery());
        } catch (Exception e) {
            log.error("Failed to publish search event: userId={}, query={}", userId, request.getSearchQuery(), e);
            // Don't throw - analytics events are fire-and-forget
        }
    }

    /**
     * Publishes a product view event to RabbitMQ.
     * Called when user views a product for a duration.
     */
    public void publishProductViewEvent(String userId, ProductViewEventRequest request) {
        ProductViewEventMessage message = ProductViewEventMessage.builder()
            .eventId(UUID.randomUUID().toString())
            .userId(userId)
            .productId(request.getProductId())
            .sessionId(request.getSessionId())
            .durationSeconds(request.getDurationSeconds())
            .source(request.getSource())
            .timestamp(Instant.now())
            .build();

        try {
            rabbitTemplate.convertAndSend(
                AnalyticsRabbitConfig.USER_ACTIVITY_EXCHANGE,
                AnalyticsRabbitConfig.VIEW_ROUTING_KEY,
                message
            );
            log.debug("Published product view event: userId={}, productId={}, duration={}",
                userId, request.getProductId(), request.getDurationSeconds());
        } catch (Exception e) {
            log.error("Failed to publish product view event: userId={}, productId={}",
                userId, request.getProductId(), e);
            // Don't throw - analytics events are fire-and-forget
        }
    }
}

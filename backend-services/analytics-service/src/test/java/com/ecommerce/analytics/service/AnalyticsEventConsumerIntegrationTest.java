package com.ecommerce.analytics.service;

import com.ecommerce.analytics.domain.ProductViewEvent;
import com.ecommerce.analytics.domain.RecommendationFeedbackEvent;
import com.ecommerce.analytics.domain.UserSearchEvent;
import com.ecommerce.analytics.repository.ProductViewEventRepository;
import com.ecommerce.analytics.repository.RecommendationFeedbackEventRepository;
import com.ecommerce.analytics.repository.UserSearchEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AnalyticsEventConsumerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("analytics_test_db")
        .withUsername("test_user")
        .withPassword("test_password");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserSearchEventRepository searchEventRepository;

    @Autowired
    private ProductViewEventRepository viewEventRepository;

    @Autowired
    private RecommendationFeedbackEventRepository feedbackEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSearchEventConsumption() {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("userId", "user-123");
        event.put("searchQuery", "summer wedding dress");
        event.put("resultCount", 12);
        event.put("clickedProductId", 42);
        event.put("timestamp", Instant.now().toString());

        rabbitTemplate.convertAndSend("user-activity.exchange", "user.search", event);

        await().until(() -> searchEventRepository.findAll().size() > 0);

        UserSearchEvent saved = searchEventRepository.findAll().get(0);
        assertEquals("user-123", saved.getUserId());
        assertEquals("summer wedding dress", saved.getSearchQuery());
        assertEquals(12, saved.getResultCount());
        assertEquals(42, saved.getClickedProductId());
    }

    @Test
    void testProductViewEventConsumption() {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("userId", "user-456");
        event.put("productId", 99);
        event.put("sessionId", "sess-xyz");
        event.put("durationSeconds", 45);
        event.put("source", "search");
        event.put("timestamp", Instant.now().toString());

        rabbitTemplate.convertAndSend("user-activity.exchange", "user.product.viewed", event);

        await().until(() -> viewEventRepository.findAll().size() > 0);

        ProductViewEvent saved = viewEventRepository.findAll().get(0);
        assertEquals("user-456", saved.getUserId());
        assertEquals(99, saved.getProductId());
        assertEquals("sess-xyz", saved.getSessionId());
        assertEquals(45, saved.getDurationSeconds());
        assertEquals("search", saved.getSource());
    }

    @Test
    void testRecommendationFeedbackConsumption() {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("userId", "user-789");
        event.put("productId", 55);
        event.put("recommendationId", "rec-batch-001");
        event.put("action", "purchased");
        event.put("orderId", 888);
        event.put("timestamp", Instant.now().toString());

        rabbitTemplate.convertAndSend("user-activity.exchange", "recommendation.feedback", event);

        await().until(() -> feedbackEventRepository.findAll().size() > 0);

        RecommendationFeedbackEvent saved = feedbackEventRepository.findAll().get(0);
        assertEquals("user-789", saved.getUserId());
        assertEquals(55, saved.getProductId());
        assertEquals("rec-batch-001", saved.getRecommendationId());
        assertEquals("purchased", saved.getAction());
        assertEquals(888, saved.getOrderId());
    }
}

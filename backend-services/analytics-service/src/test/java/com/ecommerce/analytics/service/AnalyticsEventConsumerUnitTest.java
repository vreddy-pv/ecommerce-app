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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventConsumerUnitTest {

    @Mock
    private UserSearchEventRepository searchEventRepository;

    @Mock
    private ProductViewEventRepository viewEventRepository;

    @Mock
    private RecommendationFeedbackEventRepository feedbackEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnalyticsEventConsumer analyticsEventConsumer;

    @BeforeEach
    void setUp() {
        analyticsEventConsumer = new AnalyticsEventConsumer(searchEventRepository, viewEventRepository, feedbackEventRepository, objectMapper);
    }

    @Test
    void testOnSearchEvent_ParsesAndSavesEvent() throws Exception {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("userId", "user-123");
        event.put("searchQuery", "summer wedding dress");
        event.put("resultCount", 12);
        event.put("clickedProductId", 42);
        event.put("timestamp", Instant.now().toString());

        analyticsEventConsumer.onSearchEvent(objectMapper.writeValueAsString(event));

        ArgumentCaptor<UserSearchEvent> captor = ArgumentCaptor.forClass(UserSearchEvent.class);
        verify(searchEventRepository).save(captor.capture());

        UserSearchEvent saved = captor.getValue();
        assertEquals("user-123", saved.getUserId());
        assertEquals("summer wedding dress", saved.getSearchQuery());
        assertEquals(12, saved.getResultCount());
        assertEquals(42, saved.getClickedProductId());
    }

    @Test
    void testOnProductViewEvent_ParsesAndSavesEvent() throws Exception {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("userId", "user-456");
        event.put("productId", 99);
        event.put("sessionId", "sess-xyz");
        event.put("durationSeconds", 45);
        event.put("source", "search");
        event.put("timestamp", Instant.now().toString());

        analyticsEventConsumer.onProductViewEvent(objectMapper.writeValueAsString(event));

        ArgumentCaptor<ProductViewEvent> captor = ArgumentCaptor.forClass(ProductViewEvent.class);
        verify(viewEventRepository).save(captor.capture());

        ProductViewEvent saved = captor.getValue();
        assertEquals("user-456", saved.getUserId());
        assertEquals(99, saved.getProductId());
        assertEquals("sess-xyz", saved.getSessionId());
        assertEquals(45, saved.getDurationSeconds());
        assertEquals("search", saved.getSource());
    }

    @Test
    void testOnRecommendationFeedback_ParsesAndSavesEvent() throws Exception {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("userId", "user-789");
        event.put("productId", 55);
        event.put("recommendationId", "rec-batch-001");
        event.put("action", "purchased");
        event.put("orderId", 888);
        event.put("timestamp", Instant.now().toString());

        analyticsEventConsumer.onRecommendationFeedback(objectMapper.writeValueAsString(event));

        ArgumentCaptor<RecommendationFeedbackEvent> captor = ArgumentCaptor.forClass(RecommendationFeedbackEvent.class);
        verify(feedbackEventRepository).save(captor.capture());

        RecommendationFeedbackEvent saved = captor.getValue();
        assertEquals("user-789", saved.getUserId());
        assertEquals(55, saved.getProductId());
        assertEquals("rec-batch-001", saved.getRecommendationId());
        assertEquals("purchased", saved.getAction());
        assertEquals(888, saved.getOrderId());
    }

    @Test
    void testSearchEvent_WithNullClickedProductId() throws Exception {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("userId", "user-123");
        event.put("searchQuery", "blue dress");
        event.put("resultCount", 25);
        event.putNull("clickedProductId");
        event.put("timestamp", Instant.now().toString());

        analyticsEventConsumer.onSearchEvent(objectMapper.writeValueAsString(event));

        ArgumentCaptor<UserSearchEvent> captor = ArgumentCaptor.forClass(UserSearchEvent.class);
        verify(searchEventRepository).save(captor.capture());

        UserSearchEvent saved = captor.getValue();
        assertNull(saved.getClickedProductId());
    }

    @Test
    void testProductViewEvent_AnonymousUser() throws Exception {
        ObjectNode event = objectMapper.createObjectNode();
        event.putNull("userId");
        event.put("productId", 42);
        event.put("sessionId", "sess-anon-123");
        event.put("durationSeconds", 30);
        event.put("source", "direct");
        event.put("timestamp", Instant.now().toString());

        analyticsEventConsumer.onProductViewEvent(objectMapper.writeValueAsString(event));

        ArgumentCaptor<ProductViewEvent> captor = ArgumentCaptor.forClass(ProductViewEvent.class);
        verify(viewEventRepository).save(captor.capture());

        ProductViewEvent saved = captor.getValue();
        assertNull(saved.getUserId());
        assertEquals(42, saved.getProductId());
    }

    @Test
    void testFeedbackEvent_WithoutOrderId() throws Exception {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("userId", "user-123");
        event.put("productId", 42);
        event.put("recommendationId", "rec-001");
        event.put("action", "dismissed");
        event.putNull("orderId");
        event.put("timestamp", Instant.now().toString());

        analyticsEventConsumer.onRecommendationFeedback(objectMapper.writeValueAsString(event));

        ArgumentCaptor<RecommendationFeedbackEvent> captor = ArgumentCaptor.forClass(RecommendationFeedbackEvent.class);
        verify(feedbackEventRepository).save(captor.capture());

        RecommendationFeedbackEvent saved = captor.getValue();
        assertEquals("dismissed", saved.getAction());
        assertNull(saved.getOrderId());
    }
}

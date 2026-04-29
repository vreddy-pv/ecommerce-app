package com.ecommerce.aggregator.controller;

import com.ecommerce.aggregator.config.AnalyticsRabbitConfig;
import com.ecommerce.aggregator.config.SecurityConfig;
import com.ecommerce.aggregator.dto.ProductViewEventRequest;
import com.ecommerce.aggregator.dto.SearchEventRequest;
import com.ecommerce.aggregator.service.AnalyticsEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import static org.mockito.Mockito.mock;

@WebMvcTest(AnalyticsController.class)
@Import({SecurityConfig.class, AnalyticsRabbitConfig.class, AnalyticsControllerTest.TestRabbitConfig.class})
class AnalyticsControllerTest {

    @TestConfiguration
    static class TestRabbitConfig {
        @Bean
        public ConnectionFactory connectionFactory() {
            return mock(ConnectionFactory.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsEventService analyticsEventService;

    @Test
    void logSearchEventReturns202Accepted() throws Exception {
        SearchEventRequest request = SearchEventRequest.builder()
            .searchQuery("summer wedding dress")
            .resultCount(12)
            .clickedProductId(null)
            .build();

        String requestBody = """
            {
                "searchQuery": "summer wedding dress",
                "resultCount": 12
            }
            """;

        mockMvc.perform(post("/api/analytics/search-event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-User-Id", "user-123"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.success").value(true));

        verify(analyticsEventService).publishSearchEvent(eq("user-123"), any(SearchEventRequest.class));
    }

    @Test
    void logSearchEventWithClickReturns202() throws Exception {
        String requestBody = """
            {
                "searchQuery": "summer wedding dress",
                "resultCount": 12,
                "clickedProductId": 42
            }
            """;

        mockMvc.perform(post("/api/analytics/search-event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-User-Id", "user-456"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.success").value(true));

        verify(analyticsEventService).publishSearchEvent(eq("user-456"), any(SearchEventRequest.class));
    }

    @Test
    void logSearchEventWithoutQueryReturnsBadRequest() throws Exception {
        String requestBody = """
            {
                "resultCount": 12
            }
            """;

        mockMvc.perform(post("/api/analytics/search-event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-User-Id", "user-123"))
            .andExpect(status().isBadRequest());

        verify(analyticsEventService, never()).publishSearchEvent(anyString(), any());
    }

    @Test
    void logProductViewEventReturns202Accepted() throws Exception {
        String requestBody = """
            {
                "productId": 42,
                "sessionId": "sess-xyz",
                "durationSeconds": 45,
                "source": "search"
            }
            """;

        mockMvc.perform(post("/api/analytics/product-view-event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-User-Id", "user-789"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.success").value(true));

        verify(analyticsEventService).publishProductViewEvent(eq("user-789"), any(ProductViewEventRequest.class));
    }

    @Test
    void logProductViewEventWithDefaultSourceReturns202() throws Exception {
        String requestBody = """
            {
                "productId": 42,
                "sessionId": "sess-xyz",
                "durationSeconds": 45
            }
            """;

        mockMvc.perform(post("/api/analytics/product-view-event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-User-Id", "user-789"))
            .andExpect(status().isAccepted());

        verify(analyticsEventService).publishProductViewEvent(eq("user-789"), any(ProductViewEventRequest.class));
    }

    @Test
    void logProductViewEventWithoutProductIdReturnsBadRequest() throws Exception {
        String requestBody = """
            {
                "sessionId": "sess-xyz",
                "durationSeconds": 45
            }
            """;

        mockMvc.perform(post("/api/analytics/product-view-event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-User-Id", "user-789"))
            .andExpect(status().isBadRequest());

        verify(analyticsEventService, never()).publishProductViewEvent(anyString(), any());
    }
}

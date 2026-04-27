package com.ecommerce.aggregator.service;

import com.ecommerce.aggregator.dto.DashboardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class DashboardAggregatorServiceTest {

    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private DashboardAggregatorService aggregatorService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        aggregatorService = new DashboardAggregatorService(webClientBuilder);
        setField(aggregatorService, "catalogUrl",   "http://catalog-service:8083");
        setField(aggregatorService, "inventoryUrl", "http://inventory-service:8084");
        setField(aggregatorService, "orderUrl",     "http://order-service:8085");
        setField(aggregatorService, "userUrl",      "http://user-service:8082");
    }

    @Test
    void allServicesRespondReturnsCombinedDashboard() {
        when(responseSpec.bodyToMono(Object.class)).thenReturn(Mono.just("DATA"));

        DashboardResponse result = aggregatorService.aggregate(42L);

        assertThat(result.getFeaturedProducts()).isEqualTo("DATA");
        assertThat(result.getInventoryStatus()).isEqualTo("DATA");
        assertThat(result.getRecentOrders()).isEqualTo("DATA");
        assertThat(result.getUserProfile()).isEqualTo("DATA");
    }

    @Test
    void downstreamFailureReturnsFallback() {
        when(responseSpec.bodyToMono(Object.class))
            .thenReturn(Mono.error(new RuntimeException("service down")));

        DashboardResponse result = aggregatorService.aggregate(42L);

        assertThat(result.getFeaturedProducts()).isEqualTo("UNAVAILABLE");
        assertThat(result.getInventoryStatus()).isEqualTo("UNAVAILABLE");
        assertThat(result.getRecentOrders()).isEqualTo("UNAVAILABLE");
        assertThat(result.getUserProfile()).isEqualTo("UNAVAILABLE");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

package com.ecommerce.orderprocessing.service;

import com.ecommerce.orderprocessing.client.InventoryClient;
import com.ecommerce.orderprocessing.config.RabbitMqConfig;
import com.ecommerce.orderprocessing.domain.JobStatus;
import com.ecommerce.orderprocessing.domain.ProcessingJob;
import com.ecommerce.orderprocessing.repository.ProcessingJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock private ProcessingJobRepository jobRepo;
    @Mock private InventoryClient inventoryClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private OrderProcessingService processingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        processingService = new OrderProcessingService(jobRepo, inventoryClient, rabbitTemplate, objectMapper);
    }

    private String orderCreatedPayload(long orderId) {
        return """
            {"orderId":%d,"userId":42,"totalAmount":59.98,"items":[{"productId":100,"quantity":2}]}
            """.formatted(orderId).strip();
    }

    @Test
    void happyPathPublishesConfirmedEvent() {
        when(jobRepo.existsByOrderId(1L)).thenReturn(false);
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(inventoryClient).reserve(any(), any());

        processingService.onOrderCreated(orderCreatedPayload(1L));

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMqConfig.ORDERS_EXCHANGE),
            routingKeyCaptor.capture(),
            anyString());
        assertThat(routingKeyCaptor.getValue()).isEqualTo(RabbitMqConfig.ORDER_CONFIRMED_KEY);
    }

    @Test
    void inventoryFailurePublishesFailedEvent() {
        when(jobRepo.existsByOrderId(2L)).thenReturn(false);
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("insufficient stock")).when(inventoryClient).reserve(any(), any());

        assertThatThrownBy(() -> processingService.onOrderCreated(orderCreatedPayload(2L)));

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMqConfig.ORDERS_EXCHANGE),
            routingKeyCaptor.capture(),
            anyString());
        assertThat(routingKeyCaptor.getValue()).isEqualTo(RabbitMqConfig.ORDER_FAILED_KEY);
    }

    @Test
    void duplicateEventIsSkipped() {
        when(jobRepo.existsByOrderId(3L)).thenReturn(true);

        processingService.onOrderCreated(orderCreatedPayload(3L));

        verify(inventoryClient, never()).reserve(any(), any());
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void jobStatusSetToConfirmedOnSuccess() {
        when(jobRepo.existsByOrderId(4L)).thenReturn(false);
        ArgumentCaptor<ProcessingJob> captor = ArgumentCaptor.forClass(ProcessingJob.class);
        when(jobRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(inventoryClient).reserve(any(), any());

        processingService.onOrderCreated(orderCreatedPayload(4L));

        // Last save should have CONFIRMED status
        ProcessingJob lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(JobStatus.CONFIRMED);
    }

    @Test
    void jobStatusSetToFailedOnInventoryError() {
        when(jobRepo.existsByOrderId(5L)).thenReturn(false);
        ArgumentCaptor<ProcessingJob> captor = ArgumentCaptor.forClass(ProcessingJob.class);
        when(jobRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("out of stock")).when(inventoryClient).reserve(any(), any());

        assertThatThrownBy(() -> processingService.onOrderCreated(orderCreatedPayload(5L)));

        ProcessingJob lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(lastSaved.getLastError()).contains("out of stock");
    }
}

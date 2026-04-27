package com.ecommerce.orderprocessing.service;

import com.ecommerce.orderprocessing.client.InventoryClient;
import com.ecommerce.orderprocessing.config.RabbitMqConfig;
import com.ecommerce.orderprocessing.domain.JobStatus;
import com.ecommerce.orderprocessing.domain.ProcessingJob;
import com.ecommerce.orderprocessing.repository.ProcessingJobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final ProcessingJobRepository jobRepo;
    private final InventoryClient inventoryClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREATED_QUEUE)
    @Transactional
    @SneakyThrows
    public void onOrderCreated(String payload) {
        JsonNode node = objectMapper.readTree(payload);
        Long orderId = node.get("orderId").asLong();
        Long userId  = node.has("userId") ? node.get("userId").asLong() : null;

        // Idempotency: skip if already processed
        if (jobRepo.existsByOrderId(orderId)) {
            log.info("Order {} already processed — skipping", orderId);
            return;
        }

        ProcessingJob job = ProcessingJob.builder()
            .orderId(orderId)
            .status(JobStatus.PROCESSING)
            .build();
        jobRepo.save(job);

        try {
            List<Map<String, Object>> items = extractItems(node);
            inventoryClient.reserve(orderId, items);

            job.setStatus(JobStatus.CONFIRMED);
            jobRepo.save(job);

            String confirmPayload = objectMapper.writeValueAsString(
                Map.of("orderId", orderId, "userId", userId));
            rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDERS_EXCHANGE,
                RabbitMqConfig.ORDER_CONFIRMED_KEY,
                confirmPayload);

            log.info("Order {} confirmed", orderId);
        } catch (Exception ex) {
            job.setStatus(JobStatus.FAILED);
            job.setAttemptCount(job.getAttemptCount() + 1);
            job.setLastError(ex.getMessage());
            jobRepo.save(job);

            String failPayload = objectMapper.writeValueAsString(
                Map.of("orderId", orderId, "reason", ex.getMessage()));
            rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDERS_EXCHANGE,
                RabbitMqConfig.ORDER_FAILED_KEY,
                failPayload);

            log.warn("Order {} failed: {}", orderId, ex.getMessage());
            throw ex;
        }
    }

    @SneakyThrows
    private List<Map<String, Object>> extractItems(JsonNode node) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (node.has("items")) {
            for (JsonNode item : node.get("items")) {
                items.add(Map.of(
                    "productId", item.get("productId").asLong(),
                    "quantity", item.get("quantity").asInt()
                ));
            }
        }
        return items;
    }
}

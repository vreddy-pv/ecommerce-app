package com.ecommerce.order.service;

import com.ecommerce.order.config.RabbitMqConfig;
import com.ecommerce.order.domain.OutboxEvent;
import com.ecommerce.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final OutboxEventRepository outboxRepo;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void publishPending() {
        var pending = outboxRepo.findByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            try {
                rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ORDERS_EXCHANGE,
                    event.getEventType(),
                    event.getPayload()
                );
                event.setPublished(true);
                outboxRepo.save(event);
                log.debug("Published outbox event {} type={}", event.getId(), event.getEventType());
            } catch (Exception ex) {
                log.warn("Failed to publish outbox event {}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}

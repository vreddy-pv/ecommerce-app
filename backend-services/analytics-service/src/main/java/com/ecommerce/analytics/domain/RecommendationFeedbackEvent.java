package com.ecommerce.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "recommendation_feedback_events", indexes = {
    @Index(name = "idx_feedback_user_id", columnList = "user_id, created_at"),
    @Index(name = "idx_feedback_recommendation_id", columnList = "recommendation_id"),
    @Index(name = "idx_feedback_action", columnList = "action")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationFeedbackEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 36)
    private String recommendationId;

    @Column(nullable = false, length = 20)
    private String action;

    private Long orderId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

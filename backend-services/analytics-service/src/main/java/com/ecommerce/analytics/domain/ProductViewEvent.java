package com.ecommerce.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "product_view_events", indexes = {
    @Index(name = "idx_view_events_user_id", columnList = "user_id, created_at"),
    @Index(name = "idx_view_events_product_id", columnList = "product_id, created_at"),
    @Index(name = "idx_view_events_session", columnList = "session_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductViewEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36)
    private String userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 36)
    private String sessionId;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

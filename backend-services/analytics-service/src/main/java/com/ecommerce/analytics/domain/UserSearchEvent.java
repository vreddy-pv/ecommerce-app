package com.ecommerce.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_search_events", indexes = {
    @Index(name = "idx_search_events_user_id", columnList = "user_id, created_at"),
    @Index(name = "idx_search_events_query", columnList = "search_query, created_at"),
    @Index(name = "idx_search_events_clicked", columnList = "clicked_product_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 200)
    private String searchQuery;

    @Column(nullable = false)
    private Integer resultCount;

    private Long clickedProductId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

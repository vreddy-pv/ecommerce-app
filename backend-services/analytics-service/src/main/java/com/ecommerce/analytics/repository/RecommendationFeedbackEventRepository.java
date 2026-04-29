package com.ecommerce.analytics.repository;

import com.ecommerce.analytics.domain.RecommendationFeedbackEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationFeedbackEventRepository extends JpaRepository<RecommendationFeedbackEvent, Long> {
}

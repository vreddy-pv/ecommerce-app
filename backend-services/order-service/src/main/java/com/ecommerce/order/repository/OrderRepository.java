package com.ecommerce.order.repository;

import com.ecommerce.common.model.OrderStatus;
import com.ecommerce.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    /** Admin summary: [status, count, sum] rows for orders created since the given instant. */
    @Query("SELECT o.status, COUNT(o), SUM(o.totalAmount) FROM Order o WHERE o.createdAt >= :since GROUP BY o.status")
    List<Object[]> getSummaryByStatus(@Param("since") Instant since);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findTop20ByOrderByCreatedAtDesc();
}

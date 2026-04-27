package com.ecommerce.orderprocessing.repository;

import com.ecommerce.orderprocessing.domain.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {
    Optional<ProcessingJob> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
}

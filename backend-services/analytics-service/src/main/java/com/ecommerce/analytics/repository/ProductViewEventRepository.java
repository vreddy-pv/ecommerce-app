package com.ecommerce.analytics.repository;

import com.ecommerce.analytics.domain.ProductViewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductViewEventRepository extends JpaRepository<ProductViewEvent, Long> {
}

package com.ecommerce.analytics.repository;

import com.ecommerce.analytics.domain.UserSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSearchEventRepository extends JpaRepository<UserSearchEvent, Long> {
}

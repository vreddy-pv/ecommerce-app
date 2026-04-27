package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.domain.Reservation;
import com.ecommerce.inventory.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'RESERVED' AND r.expiresAt < :now")
    List<Reservation> findExpiredReservations(@Param("now") Instant now);
}

package com.ecommerce.inventory.service;

import com.ecommerce.common.exception.InsufficientInventoryException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.domain.Reservation;
import com.ecommerce.inventory.domain.ReservationStatus;
import com.ecommerce.inventory.dto.ReserveRequest;
import com.ecommerce.inventory.dto.StockDto;
import com.ecommerce.inventory.repository.InventoryItemRepository;
import com.ecommerce.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryItemRepository inventoryRepo;
    private final ReservationRepository reservationRepo;

    @Cacheable(value = "inventory", key = "#productId")
    public StockDto getStock(Long productId) {
        var item = inventoryRepo.findByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", productId));
        int available = item.getQuantity() - item.getReservedQuantity();
        return new StockDto(productId, item.getQuantity(), item.getReservedQuantity(), available, available > 0);
    }

    @Transactional
    @CacheEvict(value = "inventory", allEntries = true)
    public void reserve(ReserveRequest req) {
        for (var lineItem : req.items()) {
            var item = inventoryRepo.findByProductIdForUpdate(lineItem.productId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", lineItem.productId()));

            int available = item.getQuantity() - item.getReservedQuantity();
            if (lineItem.quantity() > available) {
                throw new InsufficientInventoryException(lineItem.productId(), lineItem.quantity(), available);
            }

            item.setReservedQuantity(item.getReservedQuantity() + lineItem.quantity());

            var reservation = Reservation.builder()
                .orderId(req.orderId())
                .productId(lineItem.productId())
                .reservedQty(lineItem.quantity())
                .status(ReservationStatus.RESERVED)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(30 * 60))
                .build();

            reservationRepo.save(reservation);
            inventoryRepo.save(item);
        }
    }

    @Transactional
    @CacheEvict(value = "inventory", allEntries = true)
    public void release(Long orderId) {
        var reservations = reservationRepo.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        for (var reservation : reservations) {
            var item = inventoryRepo.findByProductId(reservation.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", reservation.getProductId()));
            item.setReservedQuantity(item.getReservedQuantity() - reservation.getReservedQty());
            reservation.setStatus(ReservationStatus.RELEASED);
            inventoryRepo.save(item);
            reservationRepo.save(reservation);
        }
    }

    @Transactional
    @CacheEvict(value = "inventory", allEntries = true)
    public void confirm(Long orderId) {
        var reservations = reservationRepo.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        for (var reservation : reservations) {
            var item = inventoryRepo.findByProductId(reservation.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", reservation.getProductId()));
            item.setQuantity(item.getQuantity() - reservation.getReservedQty());
            item.setReservedQuantity(item.getReservedQuantity() - reservation.getReservedQty());
            reservation.setStatus(ReservationStatus.CONFIRMED);
            inventoryRepo.save(item);
            reservationRepo.save(reservation);
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 300_000)
    public void releaseExpiredReservations() {
        var expired = reservationRepo.findExpiredReservations(Instant.now());
        for (var reservation : expired) {
            var item = inventoryRepo.findByProductId(reservation.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", reservation.getProductId()));
            item.setReservedQuantity(item.getReservedQuantity() - reservation.getReservedQty());
            reservation.setStatus(ReservationStatus.EXPIRED);
            inventoryRepo.save(item);
            reservationRepo.save(reservation);
        }
    }
}

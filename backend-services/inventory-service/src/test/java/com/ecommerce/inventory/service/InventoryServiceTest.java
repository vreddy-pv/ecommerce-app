package com.ecommerce.inventory.service;

import com.ecommerce.common.exception.InsufficientInventoryException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.domain.InventoryItem;
import com.ecommerce.inventory.domain.Reservation;
import com.ecommerce.inventory.domain.ReservationStatus;
import com.ecommerce.inventory.dto.ReserveRequest;
import com.ecommerce.inventory.dto.StockDto;
import com.ecommerce.inventory.repository.InventoryItemRepository;
import com.ecommerce.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryItemRepository inventoryRepo;
    @Mock private ReservationRepository reservationRepo;
    @InjectMocks private InventoryService inventoryService;

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        item = InventoryItem.builder()
            .id(1L).productId(100L)
            .quantity(10).reservedQuantity(0)
            .reorderThreshold(2)
            .build();
    }

    // ── Stock Query ───────────────────────────────────────────────────────────

    @Test
    void getStockReturnsAvailableQuantity() {
        when(inventoryRepo.findByProductId(100L)).thenReturn(Optional.of(item));

        StockDto result = inventoryService.getStock(100L);

        assertThat(result.productId()).isEqualTo(100L);
        assertThat(result.available()).isEqualTo(10);   // quantity - reservedQuantity
        assertThat(result.reserved()).isEqualTo(0);
    }

    @Test
    void getStockThrowsWhenProductNotTracked() {
        when(inventoryRepo.findByProductId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inventoryService.getStock(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStockReflectsExistingReservations() {
        item.setReservedQuantity(3);
        when(inventoryRepo.findByProductId(100L)).thenReturn(Optional.of(item));

        StockDto result = inventoryService.getStock(100L);

        assertThat(result.available()).isEqualTo(7);   // 10 - 3
        assertThat(result.reserved()).isEqualTo(3);
    }

    // ── Reserve ───────────────────────────────────────────────────────────────

    @Test
    void reserveStockSucceedsWhenSufficientStock() {
        when(inventoryRepo.findByProductIdForUpdate(100L)).thenReturn(Optional.of(item));
        when(reservationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepo.save(any())).thenReturn(item);

        var req = new ReserveRequest(101L, List.of(new ReserveRequest.LineItem(100L, 3)));
        inventoryService.reserve(req);

        assertThat(item.getReservedQuantity()).isEqualTo(3);
        verify(reservationRepo).save(argThat(r -> r.getReservedQty() == 3));
    }

    @Test
    void reserveThrowsWhenInsufficientStock() {
        item.setQuantity(2);
        when(inventoryRepo.findByProductIdForUpdate(100L)).thenReturn(Optional.of(item));

        var req = new ReserveRequest(101L, List.of(new ReserveRequest.LineItem(100L, 5)));

        assertThatThrownBy(() -> inventoryService.reserve(req))
            .isInstanceOf(InsufficientInventoryException.class);
        assertThat(item.getReservedQuantity()).isEqualTo(0); // unchanged
    }

    @Test
    void reserveThrowsWhenAvailableIsZero() {
        item.setQuantity(5);
        item.setReservedQuantity(5); // all reserved
        when(inventoryRepo.findByProductIdForUpdate(100L)).thenReturn(Optional.of(item));

        var req = new ReserveRequest(101L, List.of(new ReserveRequest.LineItem(100L, 1)));

        assertThatThrownBy(() -> inventoryService.reserve(req))
            .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void reserveMultipleItemsRollsBackAllOnFailure() {
        InventoryItem item2 = InventoryItem.builder()
            .id(2L).productId(200L).quantity(1).reservedQuantity(0).build();

        when(inventoryRepo.findByProductIdForUpdate(100L)).thenReturn(Optional.of(item));
        when(inventoryRepo.findByProductIdForUpdate(200L)).thenReturn(Optional.of(item2));

        // item2 has only 1 but we request 5 → should throw, item should not be modified
        var req = new ReserveRequest(101L, List.of(
            new ReserveRequest.LineItem(100L, 2),
            new ReserveRequest.LineItem(200L, 5)
        ));

        assertThatThrownBy(() -> inventoryService.reserve(req))
            .isInstanceOf(InsufficientInventoryException.class);
    }

    // ── Release ───────────────────────────────────────────────────────────────

    @Test
    void releaseRestoresReservedQuantity() {
        item.setReservedQuantity(3);
        Reservation reservation = Reservation.builder()
            .id(1L).orderId(101L).productId(100L)
            .reservedQty(3).status(ReservationStatus.RESERVED)
            .build();

        when(reservationRepo.findByOrderIdAndStatus(101L, ReservationStatus.RESERVED))
            .thenReturn(List.of(reservation));
        when(inventoryRepo.findByProductId(100L)).thenReturn(Optional.of(item));
        when(inventoryRepo.save(any())).thenReturn(item);

        inventoryService.release(101L);

        assertThat(item.getReservedQuantity()).isEqualTo(0);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    // ── Confirm ───────────────────────────────────────────────────────────────

    @Test
    void confirmDeductsFromTotalQuantity() {
        item.setReservedQuantity(3);
        Reservation reservation = Reservation.builder()
            .id(1L).orderId(101L).productId(100L)
            .reservedQty(3).status(ReservationStatus.RESERVED)
            .build();

        when(reservationRepo.findByOrderIdAndStatus(101L, ReservationStatus.RESERVED))
            .thenReturn(List.of(reservation));
        when(inventoryRepo.findByProductId(100L)).thenReturn(Optional.of(item));
        when(inventoryRepo.save(any())).thenReturn(item);

        inventoryService.confirm(101L);

        assertThat(item.getQuantity()).isEqualTo(7);          // 10 - 3
        assertThat(item.getReservedQuantity()).isEqualTo(0);  // released from reserved
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    // ── Expiry ────────────────────────────────────────────────────────────────

    @Test
    void expiredReservationsAreReleasedByScheduler() {
        item.setReservedQuantity(3);
        Reservation expired = Reservation.builder()
            .id(1L).orderId(101L).productId(100L)
            .reservedQty(3).status(ReservationStatus.RESERVED)
            .expiresAt(Instant.now().minusSeconds(60))
            .build();

        when(reservationRepo.findExpiredReservations(any())).thenReturn(List.of(expired));
        when(inventoryRepo.findByProductId(100L)).thenReturn(Optional.of(item));

        inventoryService.releaseExpiredReservations();

        assertThat(item.getReservedQuantity()).isEqualTo(0);
        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }
}

package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientInventoryException extends ServiceException {
    public InsufficientInventoryException(Long productId, int requested, int available) {
        super(
            String.format("Insufficient stock for product %d: requested=%d, available=%d", productId, requested, available),
            "INSUFFICIENT_INVENTORY",
            HttpStatus.CONFLICT
        );
    }
}

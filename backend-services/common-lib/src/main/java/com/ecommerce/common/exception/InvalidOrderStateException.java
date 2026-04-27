package com.ecommerce.common.exception;

import com.ecommerce.common.model.OrderStatus;
import org.springframework.http.HttpStatus;

public class InvalidOrderStateException extends ServiceException {
    public InvalidOrderStateException(OrderStatus from, OrderStatus to) {
        super(
            String.format("Cannot transition order from %s to %s", from, to),
            "INVALID_ORDER_STATE_TRANSITION",
            HttpStatus.CONFLICT
        );
    }
}

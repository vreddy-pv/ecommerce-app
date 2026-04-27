CREATE TABLE IF NOT EXISTS reservations (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    reserved_qty INT NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMP,
    UNIQUE (order_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_reservations_order_id ON reservations(order_id, status);
CREATE INDEX IF NOT EXISTS idx_reservations_expires_at ON reservations(expires_at);

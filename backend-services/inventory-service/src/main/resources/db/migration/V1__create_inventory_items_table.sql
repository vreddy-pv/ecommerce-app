CREATE TABLE IF NOT EXISTS inventory_items (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT NOT NULL UNIQUE,
    quantity          INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    reorder_threshold INT NOT NULL DEFAULT 10,
    updated_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON inventory_items(product_id);

CREATE TABLE products (
    id          BIGSERIAL     PRIMARY KEY,
    sku         VARCHAR(50)   UNIQUE NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    description TEXT,
    price       NUMERIC(10,2) NOT NULL,
    category_id BIGINT        NOT NULL REFERENCES categories(id),
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_active   ON products (is_active);

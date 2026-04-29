CREATE TABLE product_attributes (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    attribute_key VARCHAR(50) NOT NULL,
    attribute_value VARCHAR(100) NOT NULL,
    confidence DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    source VARCHAR(20) NOT NULL DEFAULT 'admin',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(product_id, attribute_key, attribute_value)
);

CREATE INDEX idx_product_attributes_product_id ON product_attributes(product_id);
CREATE INDEX idx_product_attributes_key_value ON product_attributes(attribute_key, attribute_value);

---

CREATE TABLE user_search_events (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    search_query VARCHAR(200) NOT NULL,
    result_count INT NOT NULL,
    clicked_product_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_events_user_id ON user_search_events(user_id, created_at DESC);
CREATE INDEX idx_search_events_query ON user_search_events(search_query, created_at DESC);
CREATE INDEX idx_search_events_clicked ON user_search_events(clicked_product_id) WHERE clicked_product_id IS NOT NULL;

---

CREATE TABLE product_view_events (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36),
    product_id BIGINT NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    duration_seconds INT NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'search',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_view_events_user_id ON product_view_events(user_id, created_at DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_view_events_product_id ON product_view_events(product_id, created_at DESC);
CREATE INDEX idx_view_events_session ON product_view_events(session_id);

---

CREATE TABLE recommendation_feedback_events (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    product_id BIGINT NOT NULL,
    recommendation_id VARCHAR(36) NOT NULL,
    action VARCHAR(20) NOT NULL,
    order_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feedback_user_id ON recommendation_feedback_events(user_id, created_at DESC);
CREATE INDEX idx_feedback_recommendation_id ON recommendation_feedback_events(recommendation_id);
CREATE INDEX idx_feedback_action ON recommendation_feedback_events(action) WHERE action != 'presented';

---

CREATE TABLE recommendation_experiments (
    id BIGSERIAL PRIMARY KEY,
    experiment_id VARCHAR(36) NOT NULL UNIQUE,
    experiment_name VARCHAR(100) NOT NULL,
    version_a VARCHAR(100) NOT NULL,
    version_b VARCHAR(100) NOT NULL,
    segment_a_size INT NOT NULL,
    segment_b_size INT NOT NULL,
    metric_tracked VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'running',
    version_a_value DECIMAL(5,2),
    version_b_value DECIMAL(5,2),
    winner VARCHAR(20),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,

    CONSTRAINT check_segment_total CHECK (segment_a_size + segment_b_size = 100)
);

CREATE INDEX idx_experiments_status ON recommendation_experiments(status) WHERE status IN ('running', 'completed');

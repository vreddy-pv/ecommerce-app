-- Migrate user_id from BIGINT to VARCHAR(36) to store Keycloak UUIDs.
-- Existing integer values (e.g. 1, 42) become strings ("1", "42") — acceptable
-- for a dev/demo environment; production would handle data migration separately.
ALTER TABLE orders ALTER COLUMN user_id TYPE VARCHAR(36) USING user_id::TEXT;

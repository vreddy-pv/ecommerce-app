-- Creates all service-specific databases in the shared PostgreSQL instance.
-- Each service gets its own isolated database — no cross-service DB access.

CREATE DATABASE user_db;
CREATE DATABASE catalog_db;
CREATE DATABASE inventory_db;
CREATE DATABASE orders_db;
CREATE DATABASE processing_db;
CREATE DATABASE notification_db;

-- Grant all privileges to the app user on each database
GRANT ALL PRIVILEGES ON DATABASE user_db         TO app_user;
GRANT ALL PRIVILEGES ON DATABASE catalog_db      TO app_user;
GRANT ALL PRIVILEGES ON DATABASE inventory_db    TO app_user;
GRANT ALL PRIVILEGES ON DATABASE orders_db       TO app_user;
GRANT ALL PRIVILEGES ON DATABASE processing_db   TO app_user;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO app_user;

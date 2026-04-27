-- Google OAuth support: add provider columns, make password nullable
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN google_sub    VARCHAR(100);
ALTER TABLE users ADD COLUMN avatar_url    VARCHAR(500);
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_google_sub UNIQUE (google_sub);

-- ========================================================
--  MRP Database Schema (UUID-based)
--  Updated for SOLID and UUIDv7 requirements
-- ========================================================

-- Enable UUID generation functions
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ========================================================
-- USERS
-- ========================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    token VARCHAR(255)
);

-- ========================================================
-- MEDIA ENTRIES
-- ========================================================
CREATE TABLE IF NOT EXISTS media_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    media_type      VARCHAR(20) NOT NULL CHECK (media_type IN ('movie','series','game')),
    release_year    INT,
    genres          VARCHAR(200),
    age_restriction INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Useful indexes
CREATE INDEX IF NOT EXISTS ix_media_owner ON media_entries(owner_id);
CREATE INDEX IF NOT EXISTS ix_media_title ON media_entries(LOWER(title));

-- ========================================================
-- RATINGS
-- ========================================================
CREATE TABLE IF NOT EXISTS ratings (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    media_id          UUID NOT NULL REFERENCES media_entries(id) ON DELETE CASCADE,
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stars             INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment           TEXT,
    comment_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_media UNIQUE (media_id, user_id)
);

-- ========================================================
-- RATING LIKES (optional feature)
-- ========================================================
CREATE TABLE IF NOT EXISTS rating_likes (
    rating_id UUID NOT NULL REFERENCES ratings(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (rating_id, user_id)
);

-- ========================================================
-- Summary indexes
-- ========================================================
CREATE INDEX IF NOT EXISTS ix_rating_media ON ratings(media_id);
CREATE INDEX IF NOT EXISTS ix_rating_user  ON ratings(user_id);


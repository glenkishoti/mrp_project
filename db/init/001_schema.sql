CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    token VARCHAR(255)
);

-- media entries
CREATE TABLE IF NOT EXISTS media_entries (
                                             id             SERIAL PRIMARY KEY,
                                             owner_id       INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    media_type     VARCHAR(20) NOT NULL CHECK (media_type IN ('movie','series','game')),
    release_year   INT,
    genres         VARCHAR(200),                       -- comma-separated for simplicity
    age_restriction INT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS ix_media_owner   ON media_entries(owner_id);
CREATE INDEX IF NOT EXISTS ix_media_title   ON media_entries(LOWER(title));

-- ratings
CREATE TABLE IF NOT EXISTS ratings (
                                       id                SERIAL PRIMARY KEY,
                                       media_id          INT NOT NULL REFERENCES media_entries(id) ON DELETE CASCADE,
    user_id           INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stars             INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment           TEXT,
    comment_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_media UNIQUE (media_id, user_id)  -- one rating per media per user
    );

-- likes for ratings (optional, used later)
CREATE TABLE IF NOT EXISTS rating_likes (
                                            rating_id INT NOT NULL REFERENCES ratings(id) ON DELETE CASCADE,
    user_id   INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (rating_id, user_id)
    );
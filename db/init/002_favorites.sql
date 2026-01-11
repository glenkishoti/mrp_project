CREATE TABLE IF NOT EXISTS favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_id UUID NOT NULL REFERENCES media_entries(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure a user can only favorite a media entry once
    UNIQUE(user_id, media_id)
);

-- Index for fast lookups
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_media_id ON favorites(media_id);

-- Optional: Add a favorite_count column to media_entries for performance
ALTER TABLE media_entries
ADD COLUMN IF NOT EXISTS favorite_count INTEGER DEFAULT 0;

-- Function to update favorite count when favorites are added/removed
CREATE OR REPLACE FUNCTION update_favorite_count()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE media_entries
        SET favorite_count = favorite_count + 1
        WHERE id = NEW.media_id;
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE media_entries
        SET favorite_count = favorite_count - 1
        WHERE id = OLD.media_id;
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update favorite_count
CREATE TRIGGER favorites_count_trigger
AFTER INSERT OR DELETE ON favorites
FOR EACH ROW
EXECUTE FUNCTION update_favorite_count();
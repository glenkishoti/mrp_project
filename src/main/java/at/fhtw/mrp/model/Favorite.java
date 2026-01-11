package at.fhtw.mrp.model;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents a user's favorite media entry
 * Immutable model following the same pattern as other models
 */
public class Favorite {
    private final UUID id;
    private final UUID userId;
    private final UUID mediaId;
    private final Timestamp createdAt;

    /**
     * Constructor - creates a new Favorite instance
     * @param id - unique identifier
     * @param userId - ID of the user who favorited
     * @param mediaId - ID of the media entry that was favorited
     * @param createdAt - timestamp when favorite was created
     */
    public Favorite(UUID id, UUID userId, UUID mediaId, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.mediaId = mediaId;
        this.createdAt = createdAt;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
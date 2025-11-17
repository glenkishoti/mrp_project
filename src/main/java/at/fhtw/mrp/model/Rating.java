package at.fhtw.mrp.model;

import java.util.UUID;

public class Rating {
    private final UUID id;
    private final UUID mediaId;
    private final UUID userId;
    private final int stars;
    private final String comment;

    public Rating(UUID id, UUID mediaId, UUID userId, int stars, String comment) {
        this.id = id;
        this.mediaId = mediaId;
        this.userId = userId;
        this.stars = stars;
        this.comment = comment;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getStars() {
        return stars;
    }

    public String getComment() {
        return comment;
    }
}

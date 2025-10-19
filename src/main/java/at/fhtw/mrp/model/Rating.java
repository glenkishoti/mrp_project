package at.fhtw.mrp.model;

import java.time.Instant;

public class Rating {
    private final int id;
    private final int mediaId;
    private final int userId;
    private final int stars;
    private final String comment;
    private final boolean commentConfirmed;
    private final Instant createdAt;

    public Rating(int id, int mediaId, int userId, int stars, String comment,
                  boolean commentConfirmed, Instant createdAt) {
        this.id = id; this.mediaId = mediaId; this.userId = userId;
        this.stars = stars; this.comment = comment; this.commentConfirmed = commentConfirmed; this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getMediaId() { return mediaId; }
    public int getUserId() { return userId; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public boolean isCommentConfirmed() { return commentConfirmed; }
    public Instant getCreatedAt() { return createdAt; }
}

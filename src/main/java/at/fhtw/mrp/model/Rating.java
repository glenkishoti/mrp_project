package at.fhtw.mrp.model;

import java.util.UUID;

/**
 * Rating entity with comment approval status
 */
public class Rating {
    private UUID id;
    private UUID mediaId;
    private UUID userId;
    private int stars;
    private String comment;
    private String approvalStatus; // Values: 'pending', 'approved', 'rejected'

    // Constructor with approval status
    public Rating(UUID id, UUID mediaId, UUID userId, int stars, String comment, String approvalStatus) {
        this.id = id;
        this.mediaId = mediaId;
        this.userId = userId;
        this.stars = stars;
        this.comment = comment;
        this.approvalStatus = approvalStatus != null ? approvalStatus : "pending";
    }

    // Constructor without approval status (defaults to pending)
    public Rating(UUID id, UUID mediaId, UUID userId, int stars, String comment) {
        this(id, mediaId, userId, stars, comment, "pending");
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getMediaId() { return mediaId; }
    public UUID getUserId() { return userId; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public String getApprovalStatus() { return approvalStatus; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setMediaId(UUID mediaId) { this.mediaId = mediaId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setStars(int stars) { this.stars = stars; }
    public void setComment(String comment) { this.comment = comment; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    // Helper methods
    public boolean isApproved() {
        return "approved".equalsIgnoreCase(approvalStatus);
    }

    public boolean isPending() {
        return "pending".equalsIgnoreCase(approvalStatus);
    }

    public boolean isRejected() {
        return "rejected".equalsIgnoreCase(approvalStatus);
    }
}
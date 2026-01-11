package at.fhtw.mrp.service;

import at.fhtw.mrp.model.Rating;
import at.fhtw.mrp.repo.RatingRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Rating with comment approval logic
 */
public class RatingService implements IService {

    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Override
    public UUID create(UUID userId, Map<String, Object> data) throws SQLException {
        UUID mediaId = UUID.fromString((String) data.get("mediaId"));
        int stars = ((Number) data.get("stars")).intValue();
        String comment = (String) data.get("comment");

        return create(mediaId, userId, stars, comment);
    }

    @Override
    public Optional<?> get(UUID id) throws SQLException {
        return ratingRepository.findById(id);
    }

    @Override
    public List<?> list(String query) throws SQLException {
        return ratingRepository.listAll();
    }

    @Override
    public List<?> listByUser(UUID userId) throws SQLException {
        return ratingRepository.findByUserId(userId);
    }

    @Override
    public void update(UUID id, UUID userId, Map<String, Object> data) throws SQLException {
        Optional<?> existingOpt = ratingRepository.findById(id);

        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Rating not found");
        }

        Rating existing = (Rating) existingOpt.get();

        if (!existing.getUserId().equals(userId)) {
            throw new SecurityException("You can only edit your own ratings");
        }

        if (data.containsKey("stars")) {
            int stars = ((Number) data.get("stars")).intValue();
            if (stars < 1 || stars > 5) {
                throw new IllegalArgumentException("Stars must be between 1 and 5");
            }
            existing.setStars(stars);
        }

        if (data.containsKey("comment")) {
            String newComment = (String) data.get("comment");
            String oldComment = existing.getComment();
            existing.setComment(newComment);

            if (!newComment.equals(oldComment) && existing.isApproved()) {
                existing.setApprovalStatus("pending");
            }
        }

        ratingRepository.update(existing);
    }

    @Override
    public void delete(UUID id, UUID userId) throws SQLException {
        Optional<?> ratingOpt = ratingRepository.findById(id);

        if (ratingOpt.isEmpty()) {
            throw new IllegalArgumentException("Rating not found");
        }

        Rating rating = (Rating) ratingOpt.get();

        if (!rating.getUserId().equals(userId)) {
            throw new SecurityException("You can only delete your own ratings");
        }

        ratingRepository.delete(id);
    }

    @Override
    public String authenticate(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("Authentication not applicable for RatingService");
    }

    @Override
    public UUID register(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("Registration not applicable for RatingService");
    }

    // CUSTOM METHODS

    public UUID create(UUID mediaId, UUID userId, int stars, String comment) throws SQLException {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Stars must be between 1 and 5");
        }

        Rating rating = new Rating(null, mediaId, userId, stars, comment, "pending");
        ratingRepository.insert(rating);
        return rating.getId();
    }

    public List<Rating> listByMedia(UUID mediaId) throws SQLException {
        return ratingRepository.findByMediaIdApproved(mediaId);
    }

    public List<Rating> getPendingRatings() throws SQLException {
        return ratingRepository.findPendingRatings();
    }

    public void approveRating(UUID ratingId) throws SQLException {
        Optional<?> ratingOpt = ratingRepository.findById(ratingId);

        if (ratingOpt.isEmpty()) {
            throw new IllegalArgumentException("Rating not found");
        }

        ratingRepository.approveRating(ratingId);
    }

    public void rejectRating(UUID ratingId) throws SQLException {
        Optional<?> ratingOpt = ratingRepository.findById(ratingId);

        if (ratingOpt.isEmpty()) {
            throw new IllegalArgumentException("Rating not found");
        }

        ratingRepository.rejectRating(ratingId);
    }

    public double getAverageScore(UUID mediaId) throws SQLException {
        List<Rating> ratings = ratingRepository.findByMediaIdApproved(mediaId);

        if (ratings.isEmpty()) {
            return 0.0;
        }

        double sum = ratings.stream()
                .mapToInt(Rating::getStars)
                .sum();

        return sum / ratings.size();
    }
}
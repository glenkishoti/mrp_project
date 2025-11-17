package at.fhtw.mrp.service;

import at.fhtw.mrp.model.Rating;
import at.fhtw.mrp.repo.IRatingRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RatingService implements IRatingService {

    private final IRatingRepository ratings;

    public RatingService(IRatingRepository ratings) {
        this.ratings = ratings;
    }

    @Override
    public UUID create(UUID mediaId, UUID userId, int stars, String comment) throws SQLException {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("stars must be 1–5");
        }
        UUID id = UUID.randomUUID();
        Rating rating = new Rating(id, mediaId, userId, stars, comment);
        ratings.insert(rating);
        return id;
    }

    @Override
    public List<Rating> listByMedia(UUID mediaId) throws SQLException {
        return ratings.listByMedia(mediaId);
    }

    @Override
    public List<Rating> listByUser(UUID userId) throws SQLException {
        return ratings.listByUser(userId);
    }

    @Override
    public Optional<Rating> findById(UUID ratingId) throws SQLException {
        return ratings.findById(ratingId);
    }

    @Override
    public void delete(UUID ratingId, UUID userId) throws SQLException {
        // Verify ownership
        Optional<Rating> ratingOpt = ratings.findById(ratingId);
        if (ratingOpt.isEmpty()) {
            throw new IllegalArgumentException("Rating not found");
        }

        Rating rating = ratingOpt.get();
        if (!rating.getUserId().equals(userId)) {
            throw new SecurityException("You can only delete your own ratings");
        }

        // Delete from repository
        deleteRatingById(ratingId);
    }

    private void deleteRatingById(UUID ratingId) throws SQLException {
        // This calls the repository delete method
        // You'll need to add this to IRatingRepository if not present
        ratings.delete(ratingId);
    }
}
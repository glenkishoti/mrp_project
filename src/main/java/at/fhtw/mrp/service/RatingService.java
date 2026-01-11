package at.fhtw.mrp.service;

import at.fhtw.mrp.model.Rating;
import at.fhtw.mrp.repo.RatingRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

//Service implementation for Rating business logic

public class RatingService implements IService {

    private final RatingRepository ratings;

    public RatingService(RatingRepository ratings) {
        this.ratings = ratings;
    }

    @Override
    public UUID create(UUID userId, Map<String, Object> data) throws SQLException {
        UUID mediaId = UUID.fromString((String) data.get("mediaId"));
        int stars = ((Number) data.get("stars")).intValue();
        String comment = (String) data.get("comment");

        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("stars must be 1–5");
        }

        UUID id = UUID.randomUUID();
        Rating rating = new Rating(id, mediaId, userId, stars, comment);
        ratings.insert(rating);
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Rating> get(UUID id) throws SQLException {
        return (Optional<Rating>) ratings.findById(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Rating> list(String query) throws SQLException {
        return (List<Rating>) ratings.listAll();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Rating> listByUser(UUID userId) throws SQLException {
        return (List<Rating>) ratings.listByRelatedId(userId);
    }

    @Override
    public void update(UUID id, UUID userId, Map<String, Object> data) throws SQLException {
        throw new UnsupportedOperationException("Ratings cannot be updated, delete and recreate instead");
    }

    @Override
    public void delete(UUID id, UUID userId) throws SQLException {
        // Verify ownership
        Optional<?> ratingOpt = ratings.findById(id);
        if (ratingOpt.isEmpty()) {
            throw new IllegalArgumentException("Rating not found");
        }

        Rating rating = (Rating) ratingOpt.get();
        if (!rating.getUserId().equals(userId)) {
            throw new SecurityException("You can only delete your own ratings");
        }

        ratings.delete(id);
    }

    // HELPER METHODS (not from IService interface)

    public UUID create(UUID mediaId, UUID userId, int stars, String comment) throws SQLException {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("stars must be 1–5");
        }
        UUID id = UUID.randomUUID();
        Rating rating = new Rating(id, mediaId, userId, stars, comment);
        ratings.insert(rating);
        return id;
    }

    public List<Rating> listByMedia(UUID mediaId) throws SQLException {
        return ratings.listByMedia(mediaId);
    }

    public Optional<Rating> findById(UUID ratingId) throws SQLException {
        return (Optional<Rating>) ratings.findById(ratingId);
    }

    // UNUSED METHODS FROM ISERVICE (not needed in RatingService)

    @Override
    public String authenticate(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for RatingService");
    }

    @Override
    public UUID register(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for RatingService");
    }
}
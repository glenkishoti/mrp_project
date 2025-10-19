package at.fhtw.mrp.service;

import at.fhtw.mrp.model.Rating;
import at.fhtw.mrp.repo.RatingRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class RatingService {
    private final RatingRepository repo;

    public RatingService(RatingRepository repo) { this.repo = repo; }

    public int create(int mediaId, int userId, int stars, String comment) throws SQLException {
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("stars must be 1..5");
        return repo.insert(mediaId, userId, stars, comment);
    }

    public List<Rating> listByMedia(int mediaId) throws SQLException {
        return repo.listByMedia(mediaId);
    }

    public void update(int ratingId, int userId, int stars, String comment, Boolean confirm) throws SQLException {
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("stars must be 1..5");
        int n = repo.update(ratingId, userId, stars, comment, confirm);
        if (n == 0) throw new SecurityException("Not author or not found");
    }

    public void delete(int ratingId, int userId) throws SQLException {
        int n = repo.delete(ratingId, userId);
        if (n == 0) throw new SecurityException("Not author or not found");
    }

    public Optional<Rating> get(int id) throws SQLException { return repo.findById(id); }
}

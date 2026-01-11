package at.fhtw.mrp.repo;

import at.fhtw.mrp.db.Database;
import at.fhtw.mrp.model.Rating;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Rating with comment approval support
 */
public class RatingRepository implements IRepository {

    @Override
    public void insert(Object entity) throws SQLException {
        Rating rating = (Rating) entity;
        String sql = "INSERT INTO ratings (id, media_id, user_id, stars, comment, approval_status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            UUID id = UUID.randomUUID();
            stmt.setObject(1, id);
            stmt.setObject(2, rating.getMediaId());
            stmt.setObject(3, rating.getUserId());
            stmt.setInt(4, rating.getStars());
            stmt.setString(5, rating.getComment());
            stmt.setString(6, "pending");

            stmt.executeUpdate();
            rating.setId(id);
            rating.setApprovalStatus("pending");
        }
    }

    @Override
    public Optional<?> findById(UUID id) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToRating(rs));
            }
            return Optional.empty();
        }
    }

    @Override
    public void update(Object entity) throws SQLException {
        Rating rating = (Rating) entity;
        String sql = "UPDATE ratings SET stars = ?, comment = ?, approval_status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rating.getStars());
            stmt.setString(2, rating.getComment());
            stmt.setString(3, rating.getApprovalStatus());
            stmt.setObject(4, rating.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(UUID id) throws SQLException {
        String sql = "DELETE FROM ratings WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public List<?> listAll() throws SQLException {
        String sql = "SELECT * FROM ratings WHERE approval_status = 'approved'";
        List<Rating> ratings = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ratings.add(mapResultSetToRating(rs));
            }
        }
        return ratings;
    }

    @Override
    public List<?> listByQuery(String query) throws SQLException {
        throw new UnsupportedOperationException("Query search not applicable for ratings");
    }

    @Override
    public List<?> listByRelatedId(UUID relatedId) throws SQLException {
        return findByMediaIdApproved(relatedId);
    }

    @Override
    public Optional<?> findByString(String identifier) throws SQLException {
        throw new UnsupportedOperationException("String lookup not applicable for ratings");
    }

    // CUSTOM METHODS FOR APPROVAL

    public List<Rating> findByMediaIdApproved(UUID mediaId) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE media_id = ? AND approval_status = 'approved'";
        List<Rating> ratings = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, mediaId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ratings.add(mapResultSetToRating(rs));
            }
        }
        return ratings;
    }

    public List<Rating> findByUserId(UUID userId) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE user_id = ?";
        List<Rating> ratings = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ratings.add(mapResultSetToRating(rs));
            }
        }
        return ratings;
    }

    public List<Rating> findPendingRatings() throws SQLException {
        String sql = "SELECT * FROM ratings WHERE approval_status = 'pending' ORDER BY id";
        List<Rating> ratings = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ratings.add(mapResultSetToRating(rs));
            }
        }
        return ratings;
    }

    public void approveRating(UUID ratingId) throws SQLException {
        String sql = "UPDATE ratings SET approval_status = 'approved' WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, ratingId);
            stmt.executeUpdate();
        }
    }

    public void rejectRating(UUID ratingId) throws SQLException {
        String sql = "UPDATE ratings SET approval_status = 'rejected' WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, ratingId);
            stmt.executeUpdate();
        }
    }

    private Rating mapResultSetToRating(ResultSet rs) throws SQLException {
        return new Rating(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("media_id"),
                (UUID) rs.getObject("user_id"),
                rs.getInt("stars"),
                rs.getString("comment"),
                rs.getString("approval_status")
        );
    }
}
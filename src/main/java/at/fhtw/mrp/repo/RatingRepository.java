package at.fhtw.mrp.repo;

import at.fhtw.mrp.db.Database;
import at.fhtw.mrp.model.Rating;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RatingRepository implements IRatingRepository {

    private Rating mapRow(ResultSet rs) throws SQLException {
        return new Rating(
                rs.getObject("id", UUID.class),
                rs.getObject("media_id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getInt("stars"),
                rs.getString("comment")
        );
    }

    @Override
    public void insert(Rating rating) throws SQLException {
        String sql = """
                INSERT INTO ratings (id, media_id, user_id, stars, comment)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, rating.getId());
            ps.setObject(2, rating.getMediaId());
            ps.setObject(3, rating.getUserId());
            ps.setInt(4, rating.getStars());
            ps.setString(5, rating.getComment());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Rating> findById(UUID id) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        }
    }

    @Override
    public List<Rating> listByMedia(UUID mediaId) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE media_id = ? ORDER BY created_at DESC";
        List<Rating> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, mediaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    @Override
    public List<Rating> listByUser(UUID userId) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE user_id = ? ORDER BY created_at DESC";
        List<Rating> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    @Override
    public void delete(UUID ratingId) throws SQLException {
        String sql = "DELETE FROM ratings WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, ratingId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Rating not found");
            }
        }
    }
}
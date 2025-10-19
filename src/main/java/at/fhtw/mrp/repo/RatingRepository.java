package at.fhtw.mrp.repo;

import at.fhtw.mrp.db.Database;
import at.fhtw.mrp.model.Rating;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RatingRepository {

    public int insert(int mediaId, int userId, int stars, String comment) throws SQLException {
        String sql = """
          INSERT INTO ratings(media_id, user_id, stars, comment)
          VALUES (?,?,?,?) RETURNING id
        """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, mediaId);
            ps.setInt(2, userId);
            ps.setInt(3, stars);
            ps.setString(4, comment);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public Optional<Rating> findById(int id) throws SQLException {
        String sql = "SELECT id, media_id, user_id, stars, comment, comment_confirmed, created_at FROM ratings WHERE id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public List<Rating> listByMedia(int mediaId) throws SQLException {
        String sql = "SELECT id, media_id, user_id, stars, comment, comment_confirmed, created_at FROM ratings WHERE media_id=? ORDER BY created_at DESC";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, mediaId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Rating> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public int update(int id, int userId, int stars, String comment, Boolean confirm) throws SQLException {
        String sql = "UPDATE ratings SET stars=?, comment=?, comment_confirmed=COALESCE(?, comment_confirmed) WHERE id=? AND user_id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, stars);
            ps.setString(2, comment);
            if (confirm == null) ps.setNull(3, Types.BOOLEAN); else ps.setBoolean(3, confirm);
            ps.setInt(4, id);
            ps.setInt(5, userId);
            return ps.executeUpdate();
        }
    }

    public int delete(int id, int userId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM ratings WHERE id=? AND user_id=?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate();
        }
    }

    private Rating map(ResultSet rs) throws SQLException {
        return new Rating(
                rs.getInt("id"),
                rs.getInt("media_id"),
                rs.getInt("user_id"),
                rs.getInt("stars"),
                rs.getString("comment"),
                rs.getBoolean("comment_confirmed"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}

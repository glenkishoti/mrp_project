package at.fhtw.mrp.repo;

import at.fhtw.mrp.db.Database;
import at.fhtw.mrp.model.MediaEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MediaRepository {

    public int insert(int ownerId, String title, String description, String mediaType,
                      Integer releaseYear, String genres, Integer ageRestriction) throws SQLException {
        String sql = """
          INSERT INTO media_entries(owner_id, title, description, media_type, release_year, genres, age_restriction)
          VALUES (?,?,?,?,?,?,?) RETURNING id
        """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setString(4, mediaType);
            if (releaseYear == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, releaseYear);
            ps.setString(6, genres);
            if (ageRestriction == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, ageRestriction);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Optional<MediaEntry> findById(int id) throws SQLException {
        String sql = "SELECT id, owner_id, title, description, media_type, release_year, genres, age_restriction FROM media_entries WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public List<MediaEntry> findAll(String query) throws SQLException {
        String base = """
          SELECT id, owner_id, title, description, media_type, release_year, genres, age_restriction
          FROM media_entries
        """;
        String sql = (query == null || query.isBlank())
                ? base + " ORDER BY title"
                : base + " WHERE LOWER(title) LIKE ? ORDER BY title";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (sql.contains("LIKE")) ps.setString(1, "%" + query.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<MediaEntry> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public int update(int id, int ownerId, String title, String description, String mediaType,
                      Integer releaseYear, String genres, Integer ageRestriction) throws SQLException {
        String sql = """
          UPDATE media_entries
             SET title=?, description=?, media_type=?, release_year=?, genres=?, age_restriction=?
           WHERE id=? AND owner_id=?
        """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setString(3, mediaType);
            if (releaseYear == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, releaseYear);
            ps.setString(5, genres);
            if (ageRestriction == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, ageRestriction);
            ps.setInt(7, id);
            ps.setInt(8, ownerId);
            return ps.executeUpdate();
        }
    }

    public int delete(int id, int ownerId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM media_entries WHERE id=? AND owner_id=?")) {
            ps.setInt(1, id);
            ps.setInt(2, ownerId);
            return ps.executeUpdate();
        }
    }

    public Double averageScore(int mediaId) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT AVG(stars)::float FROM ratings WHERE media_id=?")) {
            ps.setInt(1, mediaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return (Double) rs.getObject(1); // may be null
                return null;
            }
        }
    }

    private MediaEntry map(ResultSet rs) throws SQLException {
        return new MediaEntry(
                rs.getInt("id"),
                rs.getInt("owner_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("media_type"),
                (Integer) rs.getObject("release_year"),
                rs.getString("genres"),
                (Integer) rs.getObject("age_restriction")
        );
    }
}

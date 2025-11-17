package at.fhtw.mrp.repo;

import at.fhtw.mrp.db.Database;
import at.fhtw.mrp.model.MediaEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MediaRepository implements IMediaRepository {

    private MediaEntry mapRow(ResultSet rs) throws SQLException {
        return new MediaEntry(
                rs.getObject("id", UUID.class),
                rs.getObject("owner_id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("media_type"),
                (Integer) rs.getObject("release_year"),
                rs.getString("genres"),
                (Integer) rs.getObject("age_restriction")
        );
    }

    @Override
    public void insert(MediaEntry entry) throws SQLException {
        String sql = """
                INSERT INTO media_entries
                (id, owner_id, title, description, media_type, release_year, genres, age_restriction)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, entry.getId());
            ps.setObject(2, entry.getOwnerId());
            ps.setString(3, entry.getTitle());
            ps.setString(4, entry.getDescription());
            ps.setString(5, entry.getMediaType());
            if (entry.getReleaseYear() == null) ps.setNull(6, Types.INTEGER);
            else ps.setInt(6, entry.getReleaseYear());
            ps.setString(7, entry.getGenres());
            if (entry.getAgeRestriction() == null) ps.setNull(8, Types.INTEGER);
            else ps.setInt(8, entry.getAgeRestriction());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<MediaEntry> findById(UUID id) throws SQLException {
        String sql = "SELECT * FROM media_entries WHERE id = ?";
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
    public List<MediaEntry> list(String query) throws SQLException {
        String sql = """
                SELECT * FROM media_entries
                WHERE ? IS NULL
                   OR title ILIKE '%' || ? || '%'
                   OR description ILIKE '%' || ? || '%'
                ORDER BY title
                """;
        List<MediaEntry> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (query == null || query.isBlank()) {
                ps.setNull(1, Types.VARCHAR);
                ps.setNull(2, Types.VARCHAR);
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(1, query);
                ps.setString(2, query);
                ps.setString(3, query);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    @Override
    public void update(MediaEntry entry) throws SQLException {
        String sql = """
                UPDATE media_entries SET
                    title = ?,
                    description = ?,
                    media_type = ?,
                    release_year = ?,
                    genres = ?,
                    age_restriction = ?
                WHERE id = ?
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, entry.getTitle());
            ps.setString(2, entry.getDescription());
            ps.setString(3, entry.getMediaType());
            if (entry.getReleaseYear() == null) ps.setNull(4, Types.INTEGER);
            else ps.setInt(4, entry.getReleaseYear());
            ps.setString(5, entry.getGenres());
            if (entry.getAgeRestriction() == null) ps.setNull(6, Types.INTEGER);
            else ps.setInt(6, entry.getAgeRestriction());
            ps.setObject(7, entry.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(UUID mediaId) throws SQLException {
        String sql = "DELETE FROM media_entries WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, mediaId);
            ps.executeUpdate();
        }
    }

    @Override
    public double averageScore(UUID mediaId) throws SQLException {
        String sql = "SELECT AVG(stars) AS avg_score FROM ratings WHERE media_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, mediaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("avg_score");
                return 0.0;
            }
        }
    }
}

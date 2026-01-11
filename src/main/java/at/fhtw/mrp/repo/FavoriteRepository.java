package at.fhtw.mrp.repo;

import at.fhtw.mrp.db.Database;
import at.fhtw.mrp.model.Favorite;
import at.fhtw.mrp.model.MediaEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL implementation of IRepository for Favorite entities
 */
public class FavoriteRepository implements IRepository {

    private Favorite mapRow(ResultSet rs) throws SQLException {
        return new Favorite(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getObject("media_id", UUID.class),
                rs.getTimestamp("created_at")
        );
    }

    private MediaEntry mapMediaRow(ResultSet rs) throws SQLException {
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
    public void insert(Object entity) throws SQLException {
        Favorite favorite = (Favorite) entity;
        String sql = """
                INSERT INTO favorites (id, user_id, media_id)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id, media_id) DO NOTHING
                """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, favorite.getId());
            ps.setObject(2, favorite.getUserId());
            ps.setObject(3, favorite.getMediaId());

            ps.executeUpdate();
        }
    }

    @Override
    public Optional<?> findById(UUID id) throws SQLException {
        String sql = "SELECT * FROM favorites WHERE id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public void update(Object entity) throws SQLException {
        throw new UnsupportedOperationException("update not supported for Favorite (delete and recreate instead)");
    }

    @Override
    public void delete(UUID id) throws SQLException {
        String sql = "DELETE FROM favorites WHERE id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<?> listAll() throws SQLException {
        String sql = "SELECT * FROM favorites ORDER BY created_at DESC";

        List<Favorite> favorites = new ArrayList<>();

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                favorites.add(mapRow(rs));
            }
        }

        return favorites;
    }

    @Override
    public List<?> listByQuery(String query) throws SQLException {
        throw new UnsupportedOperationException("listByQuery not supported for Favorite");
    }

    @Override
    public List<?> listByRelatedId(UUID relatedId) throws SQLException {
        // Used for listing favorites by user_id
        String sql = "SELECT * FROM favorites WHERE user_id = ? ORDER BY created_at DESC";

        List<Favorite> favorites = new ArrayList<>();

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, relatedId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    favorites.add(mapRow(rs));
                }
            }
        }

        return favorites;
    }

    @Override
    public Optional<?> findByString(String identifier) throws SQLException {
        throw new UnsupportedOperationException("findByString not supported for Favorite");
    }

    // HELPER METHODS (not from interface)

    public void addFavorite(UUID userId, UUID mediaId) throws SQLException {
        String sql = """
                INSERT INTO favorites (id, user_id, media_id)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id, media_id) DO NOTHING
                """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, userId);
            ps.setObject(3, mediaId);

            ps.executeUpdate();
        }
    }

    public void removeFavorite(UUID userId, UUID mediaId) throws SQLException {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND media_id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);
            ps.setObject(2, mediaId);

            ps.executeUpdate();
        }
    }

    public boolean isFavorite(UUID userId, UUID mediaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND media_id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);
            ps.setObject(2, mediaId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        }
    }

    public List<MediaEntry> getFavoriteMedia(UUID userId) throws SQLException {
        String sql = """
                SELECT m.*
                FROM media_entries m
                INNER JOIN favorites f ON m.id = f.media_id
                WHERE f.user_id = ?
                ORDER BY f.created_at DESC
                """;

        List<MediaEntry> media = new ArrayList<>();

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    media.add(mapMediaRow(rs));
                }
            }
        }

        return media;
    }

    public int getFavoriteCount(UUID mediaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM favorites WHERE media_id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, mediaId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }
}
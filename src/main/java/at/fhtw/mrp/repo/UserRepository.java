package at.fhtw.mrp.repo;

import at.fhtw.mrp.db.Database;
import at.fhtw.mrp.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL implementation of IRepository for User entities
 */
public class UserRepository implements IRepository {

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getObject("id", UUID.class),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("token")
        );
    }

    @Override
    public void insert(Object entity) throws SQLException {
        User user = (User) entity;
        String sql = """
                INSERT INTO users(id, username, password_hash)
                VALUES (?, ?, ?)
                """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());

            ps.executeUpdate();
        }
    }

    @Override
    public Optional<?> findById(UUID id) throws SQLException {
        String sql = "SELECT id, username, password_hash, token FROM users WHERE id = ?";

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
        User user = (User) entity;
        String sql = "UPDATE users SET token = ? WHERE id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, user.getToken());
            ps.setObject(2, user.getId());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(UUID id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<?> listAll() throws SQLException {
        String sql = "SELECT id, username, password_hash, token FROM users ORDER BY username";

        List<User> users = new ArrayList<>();

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }

        return users;
    }

    @Override
    public List<?> listByQuery(String query) throws SQLException {
        throw new UnsupportedOperationException("listByQuery not supported for Users");
    }

    @Override
    public List<?> listByRelatedId(UUID relatedId) throws SQLException {
        throw new UnsupportedOperationException("listByRelatedId not supported for Users");
    }

    @Override
    public Optional<?> findByString(String identifier) throws SQLException {
        // Used for finding by username or token
        String sql = "SELECT id, username, password_hash, token FROM users WHERE username = ? OR token = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, identifier);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        }
    }

    // HELPER METHODS (not from interface)

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, token FROM users WHERE username = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        }
    }

    public Optional<User> findByToken(String token) throws SQLException {
        String sql = "SELECT id, username, password_hash, token FROM users WHERE token = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, token);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        }
    }

    public void updateToken(UUID userId, String token) throws SQLException {
        String sql = "UPDATE users SET token = ? WHERE id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, token);
            ps.setObject(2, userId);

            ps.executeUpdate();
        }
    }
}
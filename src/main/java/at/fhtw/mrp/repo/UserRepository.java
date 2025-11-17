package at.fhtw.mrp.repo;

import at.fhtw.mrp.db.Database;
import at.fhtw.mrp.model.User;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class UserRepository implements IUserRepository {

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("token")
        );
    }

    @Override
    public void insert(User user) throws SQLException {
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
    public Optional<User> findById(UUID id) throws SQLException {
        String sql = "SELECT id, username, password_hash, token FROM users WHERE id = ?";
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

    @Override
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

    @Override
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

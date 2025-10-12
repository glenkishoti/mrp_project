package at.fhtw.mrp.repo;

import at.fhtw.mrp.db.Database;
import at.fhtw.mrp.model.User;

import java.sql.*;
import java.util.Optional;

public class UserRepository {

    public void insert(String username, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users(username, password_hash) VALUES (?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, token FROM users WHERE username = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
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
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    public void updateToken(String username, String token) throws SQLException {
        String sql = "UPDATE users SET token = ? WHERE username = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("token")
        );
    }

}

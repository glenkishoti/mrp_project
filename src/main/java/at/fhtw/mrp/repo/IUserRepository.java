package at.fhtw.mrp.repo;

import at.fhtw.mrp.model.User;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepository {

    void insert(User user) throws SQLException;

    Optional<User> findById(UUID id) throws SQLException;

    Optional<User> findByUsername(String username) throws SQLException;

    Optional<User> findByToken(String token) throws SQLException;

    void updateToken(UUID userId, String token) throws SQLException;
}

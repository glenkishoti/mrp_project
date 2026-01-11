package at.fhtw.mrp.service;

import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.UserRepository;
import at.fhtw.mrp.util.PasswordUtil;
import at.fhtw.mrp.util.TokenService;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


// Service implementation for Authentication business logic

public class AuthService implements IService {

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UUID register(String username, String password) throws SQLException {
        // Hash password
        String hash = PasswordUtil.hash(password.toCharArray());

        // Create new user object
        User u = new User(UUID.randomUUID(), username, hash, null);

        // Store user
        users.insert(u);

        return u.getId();
    }

    @Override
    public String authenticate(String username, String password) throws SQLException {
        Optional<User> opt = users.findByUsername(username);
        if (opt.isEmpty())
            throw new IllegalArgumentException("Invalid credentials");

        User user = opt.get();

        // Verify password
        boolean ok = PasswordUtil.verify(password.toCharArray(), user.getPasswordHash());
        if (!ok)
            throw new IllegalArgumentException("Invalid credentials");

        // Create token
        String token = TokenService.generateToken(user.getId(), username);

        // Store token in DB
        users.updateToken(user.getId(), token);

        return token;
    }

    // UNUSED METHODS FROM ISERVICE (not needed in AuthService)

    @Override
    public UUID create(UUID userId, Map<String, Object> data) throws SQLException {
        throw new UnsupportedOperationException("Use register() instead");
    }

    @Override
    public Optional<?> get(UUID id) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for AuthService");
    }

    @Override
    public List<?> list(String query) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for AuthService");
    }

    @Override
    public List<?> listByUser(UUID userId) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for AuthService");
    }

    @Override
    public void update(UUID id, UUID userId, Map<String, Object> data) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for AuthService");
    }

    @Override
    public void delete(UUID id, UUID userId) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for AuthService");
    }
}
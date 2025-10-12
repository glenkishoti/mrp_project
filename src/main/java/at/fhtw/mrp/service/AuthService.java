package at.fhtw.mrp.service;

import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.UserRepository;
import at.fhtw.mrp.util.PasswordUtil;
import at.fhtw.mrp.util.TokenService;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public class AuthService {

    private final UserRepository repo;

    public AuthService(UserRepository repo) {
        this.repo = repo;
    }

    public void register(String username, String password) throws SQLException, IllegalArgumentException {
        if (username == null || username.isBlank() || password == null || password.length() < 3) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        if (repo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }
        String hash = PasswordUtil.hash(password.toCharArray());
        repo.insert(username, hash);
    }

    public Map<String, String> login(String username, String password) throws SQLException, IllegalArgumentException {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Missing credentials.");
        }
        Optional<User> userOpt = repo.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid credentials.");
        }
        User u = userOpt.get();
        boolean ok = PasswordUtil.verify(password.toCharArray(), u.getPasswordHash());
        if (!ok) {
            throw new IllegalArgumentException("Invalid credentials.");
        }
        String token = TokenService.generateToken(username);
        repo.updateToken(username, token);
        return Map.of("token", token);
    }

}

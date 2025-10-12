package at.fhtw.mrp.util;

import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.UserRepository;

import java.sql.SQLException;
import java.util.Optional;

public final class TokenService {

    private TokenService() {}

    // The spec shows "username-mrpToken" as an example token.
    public static String generateToken(String username) {
        return username + "-mrpToken";
    }

    public static Optional<User> authenticate(String bearerToken, UserRepository repo) throws SQLException {
        if (bearerToken == null || bearerToken.isBlank()) return Optional.empty();
        // Expecting header "Authorization: Bearer <token>"
        String token = bearerToken.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        if (token.isBlank()) return Optional.empty();
        return repo.findByToken(token);
    }

}

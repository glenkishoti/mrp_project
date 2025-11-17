package at.fhtw.mrp.util;

import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.IUserRepository;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class TokenService {

    // NEW METHOD (expected by AuthService)
    public static String issueToken(UUID userId) {
        // username is not available here → use dummy
        return generateToken(userId, "unknown");
    }

    // ORIGINAL secure token generator
    public static String generateToken(UUID userId, String username) {
        String secret = UUID.randomUUID().toString();
        return userId + ";" + username + ";" + secret;
    }

    // ORIGINAL parser
    private static Optional<ParsedToken> parse(String token) {
        if (token == null || !token.contains(";"))
            return Optional.empty();

        String[] parts = token.split(";");
        if (parts.length < 2)
            return Optional.empty();

        try {
            UUID userId = UUID.fromString(parts[0]);
            String username = parts[1];
            return Optional.of(new ParsedToken(userId, username));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // ORIGINAL authenticate logic
    public static Optional<User> authenticate(String authHeader, IUserRepository users)
            throws SQLException {

        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return Optional.empty();

        String rawToken = authHeader.substring("Bearer ".length()).trim();

        Optional<ParsedToken> parsed = parse(rawToken);
        if (parsed.isEmpty()) return Optional.empty();

        Optional<User> user = users.findById(parsed.get().userId());
        if (user.isEmpty()) return Optional.empty();

        if (rawToken.equals(user.get().getToken()))
            return user;

        return Optional.empty();
    }

    // internal data holder
    private record ParsedToken(UUID userId, String username) {}
}

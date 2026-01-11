package at.fhtw.mrp.util;

import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.UserRepository;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;


//Token service for authentication and token generation

public class TokenService {


    // Issue a token for a user ID (when username not available)

    public static String issueToken(UUID userId) {
        return generateToken(userId, "unknown");
    }

    //Generate a secure token with userId, username, and secret

    public static String generateToken(UUID userId, String username) {
        String secret = UUID.randomUUID().toString();
        return userId + ";" + username + ";" + secret;
    }

    //Parse a token string into its components

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

    /**
     * Authenticate a user based on Authorization header
     * @param authHeader - the Authorization header value (should start with "Bearer ")
     * @param userRepo - repository to look up user
     * @return Optional<User> if authentication successful, empty otherwise
     */
    public static Optional<User> authenticate(String authHeader, UserRepository userRepo)
            throws SQLException {

        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return Optional.empty();

        String rawToken = authHeader.substring("Bearer ".length()).trim();

        Optional<ParsedToken> parsed = parse(rawToken);
        if (parsed.isEmpty()) return Optional.empty();

        // Get user from repository - need to cast Optional<?> to Optional<User>
        Optional<?> optionalUser = userRepo.findById(parsed.get().userId());
        if (optionalUser.isEmpty()) return Optional.empty();

        // Cast the user object
        User user = (User) optionalUser.get();

        // Verify token matches the one stored in database
        if (rawToken.equals(user.getToken()))
            return Optional.of(user);

        return Optional.empty();
    }

    /**
     * Internal data holder for parsed token components
     */
    private record ParsedToken(UUID userId, String username) {}
}
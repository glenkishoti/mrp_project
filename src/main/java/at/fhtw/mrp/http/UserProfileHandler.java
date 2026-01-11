package at.fhtw.mrp.http;

import at.fhtw.mrp.model.User;
import at.fhtw.mrp.service.UserProfileService;
import at.fhtw.mrp.repo.UserRepository;
import at.fhtw.mrp.util.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * UserProfile Handler
 * Supports: Profile, Statistics, Activity
 */
public class UserProfileHandler implements HttpHandler {

    private final UserProfileService profileService;
    private final UserRepository userRepository = new UserRepository();
    private final ObjectMapper mapper = new ObjectMapper();

    public UserProfileHandler(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            Optional<User> userOpt = authenticateUser(exchange);
            if (userOpt.isEmpty()) {
                sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
                return;
            }
            User user = userOpt.get();

            if ("GET".equals(method)) {
                if (path.endsWith("/statistics")) {
                    handleStatistics(exchange, user);
                } else if (path.endsWith("/activity")) {
                    handleActivity(exchange, user);
                } else {
                    handleProfile(exchange, user);
                }
            } else {
                sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    // Helper: Authenticate user from Bearer token
    private Optional<User> authenticateUser(HttpExchange exchange) {
        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Optional.empty();
            }
            return TokenService.authenticate(authHeader, userRepository);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    // Helper: Send JSON response
    private void sendResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = mapper.writeValueAsString(data);
        byte[] bytes = json.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleProfile(HttpExchange exchange, User user) throws IOException {
        Map<String, Object> profile = Map.of(
                "id", user.getId(),
                "username", user.getUsername()
        );
        sendResponse(exchange, 200, profile);
    }

    private void handleStatistics(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, Object> stats = profileService.getUserStatistics(user.getId());
        sendResponse(exchange, 200, stats);
    }

    private void handleActivity(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, Object> activity = profileService.getUserActivity(user.getId());
        sendResponse(exchange, 200, activity);
    }
}
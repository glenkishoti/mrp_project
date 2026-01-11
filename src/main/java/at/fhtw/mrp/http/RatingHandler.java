package at.fhtw.mrp.http;

import at.fhtw.mrp.model.Rating;
import at.fhtw.mrp.model.User;
import at.fhtw.mrp.service.RatingService;
import at.fhtw.mrp.repo.UserRepository;
import at.fhtw.mrp.util.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Standalone Rating Handler - NO BaseHandler dependency
 * Supports: Create, Read, UPDATE, Delete
 *
 * FIXED: Changed parts[2] to parts[3] for UUID extraction
 */
public class RatingHandler implements HttpHandler {

    private final RatingService ratingService;
    private final UserRepository userRepository = new UserRepository();
    private final ObjectMapper mapper = new ObjectMapper();

    // Constructor matching your Main.java signature
    public RatingHandler(RatingService ratingService, Object authService, Object userRepo) {
        this.ratingService = ratingService;
        // Ignore the extra parameters - they're not needed for the new features
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

            switch (method) {
                case "POST" -> handleCreate(exchange, user);
                case "GET" -> handleList(exchange, user);
                case "PUT" -> handleUpdate(exchange, user, path);
                case "DELETE" -> handleDelete(exchange, user, path);
                default -> sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
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

    // Helper: Parse JSON request body
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseRequestBody(HttpExchange exchange) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        if (body.length() == 0) return new HashMap<>();
        return mapper.readValue(body.toString(), Map.class);
    }

    // Helper: Get single query parameter
    private String getQueryParam(HttpExchange exchange, String paramName) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(paramName)) {
                return kv[1];
            }
        }
        return null;
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

    private void handleCreate(HttpExchange exchange, User user) throws IOException, SQLException {
        Map<String, Object> body = parseRequestBody(exchange);

        UUID mediaId = UUID.fromString((String) body.get("mediaId"));
        int stars = ((Number) body.get("stars")).intValue();
        String comment = (String) body.get("comment");

        UUID ratingId = ratingService.create(mediaId, user.getId(), stars, comment);

        sendResponse(exchange, 201, Map.of("id", ratingId, "message", "Rating created successfully"));
    }

    private void handleList(HttpExchange exchange, User user) throws IOException, SQLException {
        String mediaIdParam = getQueryParam(exchange, "mediaId");

        List<Rating> ratings;
        if (mediaIdParam != null) {
            UUID mediaId = UUID.fromString(mediaIdParam);
            ratings = ratingService.listByMedia(mediaId);
        } else {
            ratings = ratingService.listByUser(user.getId());
        }

        sendResponse(exchange, 200, ratings);
    }

    private void handleUpdate(HttpExchange exchange, User user, String path) throws IOException, SQLException {
        String[] parts = path.split("/");

        // FIXED: Changed from parts.length < 3 to parts.length < 4
        if (parts.length < 4) {
            sendResponse(exchange, 400, Map.of("error", "Rating ID required"));
            return;
        }

        // FIXED: Changed from parts[2] to parts[3]
        // Path: /api/ratings/{uuid}
        // parts[0] = ""
        // parts[1] = "api"
        // parts[2] = "ratings"
        // parts[3] = uuid ← THIS IS WHERE THE UUID IS!
        UUID ratingId = UUID.fromString(parts[3]);
        Map<String, Object> body = parseRequestBody(exchange);

        ratingService.update(ratingId, user.getId(), body);

        sendResponse(exchange, 200, Map.of("message", "Rating updated successfully", "id", ratingId));
    }

    private void handleDelete(HttpExchange exchange, User user, String path) throws IOException, SQLException {
        String[] parts = path.split("/");

        // FIXED: Changed from parts.length < 3 to parts.length < 4
        if (parts.length < 4) {
            sendResponse(exchange, 400, Map.of("error", "Rating ID required"));
            return;
        }

        // FIXED: Changed from parts[2] to parts[3]
        UUID ratingId = UUID.fromString(parts[3]);
        ratingService.delete(ratingId, user.getId());

        sendResponse(exchange, 200, Map.of("message", "Rating deleted successfully"));
    }
}
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
 * Rating Handler with Comment Approval Support
 *
 * User Endpoints:
 * - POST /api/ratings - Create rating (starts as pending)
 * - GET /api/ratings - Get my ratings (including pending)
 * - GET /api/ratings?mediaId={id} - Get approved ratings for media
 * - PUT /api/ratings/{id} - Edit rating
 * - DELETE /api/ratings/{id} - Delete rating
 *
 * Admin Endpoints:
 * - GET /api/ratings/pending - Get all pending ratings
 * - POST /api/ratings/{id}/approve - Approve rating
 * - POST /api/ratings/{id}/reject - Reject rating
 */
public class RatingHandler implements HttpHandler {

    private final RatingService ratingService;
    private final UserRepository userRepository = new UserRepository();
    private final ObjectMapper mapper = new ObjectMapper();

    public RatingHandler(RatingService ratingService, Object authService, Object userRepo) {
        this.ratingService = ratingService;
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

            // Route to appropriate handler
            if (method.equals("GET") && path.endsWith("/pending")) {
                handleGetPending(exchange);
            } else if (method.equals("POST") && path.contains("/approve")) {
                handleApprove(exchange, path);
            } else if (method.equals("POST") && path.contains("/reject")) {
                handleReject(exchange, path);
            } else {
                // Regular CRUD operations
                switch (method) {
                    case "POST" -> handleCreate(exchange, user);
                    case "GET" -> handleList(exchange, user);
                    case "PUT" -> handleUpdate(exchange, user, path);
                    case "DELETE" -> handleDelete(exchange, user, path);
                    default -> sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

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

        sendResponse(exchange, 201, Map.of(
                "id", ratingId,
                "message", "Rating created successfully (pending approval)",
                "status", "pending"
        ));
    }

    private void handleList(HttpExchange exchange, User user) throws IOException, SQLException {
        String mediaIdParam = getQueryParam(exchange, "mediaId");

        List<Rating> ratings;
        if (mediaIdParam != null) {
            UUID mediaId = UUID.fromString(mediaIdParam);
            ratings = ratingService.listByMedia(mediaId); // Only approved
        } else {
            // Cast from List<?> to List<Rating>
            @SuppressWarnings("unchecked")
            List<Rating> userRatings = (List<Rating>) ratingService.listByUser(user.getId());
            ratings = userRatings;
        }

        sendResponse(exchange, 200, ratings);
    }

    private void handleUpdate(HttpExchange exchange, User user, String path) throws IOException, SQLException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            sendResponse(exchange, 400, Map.of("error", "Rating ID required"));
            return;
        }

        UUID ratingId = UUID.fromString(parts[3]);
        Map<String, Object> body = parseRequestBody(exchange);

        ratingService.update(ratingId, user.getId(), body);

        sendResponse(exchange, 200, Map.of(
                "message", "Rating updated successfully (pending approval if comment changed)",
                "id", ratingId
        ));
    }

    private void handleDelete(HttpExchange exchange, User user, String path) throws IOException, SQLException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            sendResponse(exchange, 400, Map.of("error", "Rating ID required"));
            return;
        }

        UUID ratingId = UUID.fromString(parts[3]);
        ratingService.delete(ratingId, user.getId());

        sendResponse(exchange, 200, Map.of("message", "Rating deleted successfully"));
    }

    // ADMIN ENDPOINTS

    private void handleGetPending(HttpExchange exchange) throws IOException, SQLException {
        // For now, any authenticated user can see pending ratings

        List<Rating> pendingRatings = ratingService.getPendingRatings();

        sendResponse(exchange, 200, Map.of(
                "total", pendingRatings.size(),
                "ratings", pendingRatings
        ));
    }

    private void handleApprove(HttpExchange exchange, String path) throws IOException, SQLException {

        // Extract rating ID from path: /api/ratings/{id}/approve
        String[] parts = path.split("/");
        if (parts.length < 4) {
            sendResponse(exchange, 400, Map.of("error", "Rating ID required"));
            return;
        }

        UUID ratingId = UUID.fromString(parts[3]);
        ratingService.approveRating(ratingId);

        sendResponse(exchange, 200, Map.of(
                "message", "Rating approved successfully",
                "ratingId", ratingId
        ));
    }

    private void handleReject(HttpExchange exchange, String path) throws IOException, SQLException {

        // Extract rating ID from path: /api/ratings/{id}/reject
        String[] parts = path.split("/");
        if (parts.length < 4) {
            sendResponse(exchange, 400, Map.of("error", "Rating ID required"));
            return;
        }

        UUID ratingId = UUID.fromString(parts[3]);
        ratingService.rejectRating(ratingId);

        sendResponse(exchange, 200, Map.of(
                "message", "Rating rejected successfully",
                "ratingId", ratingId
        ));
    }
}
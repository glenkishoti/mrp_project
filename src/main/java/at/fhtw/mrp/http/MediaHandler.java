package at.fhtw.mrp.http;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.model.User;
import at.fhtw.mrp.service.MediaService;
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
 * Standalone Media Handler - NO BaseHandler dependency
 * Supports: Filtering, Sorting, Search
 */
public class MediaHandler implements HttpHandler {

    private final MediaService mediaService;
    private final UserRepository userRepository = new UserRepository();
    private final ObjectMapper mapper = new ObjectMapper();

    // Constructor matching your Main.java signature
    public MediaHandler(MediaService mediaService, Object ratingService, Object authService) {
        this.mediaService = mediaService;
        // Ignore the extra parameters - they're not needed for the new features
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "POST" -> handleCreate(exchange);
                case "GET" -> handleList(exchange, path);
                case "PUT" -> handleUpdate(exchange, path);
                case "DELETE" -> handleDelete(exchange, path);
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

    // Helper: Get all query parameters
    private Map<String, String> getAllQueryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
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

    private void handleCreate(HttpExchange exchange) throws IOException, SQLException {
        Optional<User> userOpt = authenticateUser(exchange);
        if (userOpt.isEmpty()) {
            sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
            return;
        }

        Map<String, Object> body = parseRequestBody(exchange);
        UUID mediaId = mediaService.createFromRequest(userOpt.get(), body);

        sendResponse(exchange, 201, Map.of("id", mediaId, "message", "Media created successfully"));
    }

    private void handleList(HttpExchange exchange, String path) throws IOException, SQLException {
        String[] parts = path.split("/");
        // Path: /api/media/{id} splits to ["", "api", "media", "{id}"]
        // Only call handleGet if there's actually an ID (parts[3] exists)
        if (parts.length >= 4 && !parts[3].isBlank()) {
            handleGet(exchange, parts[3]);  // Changed from parts[2] to parts[3]
            return;
        }

        Map<String, String> params = getAllQueryParams(exchange);
        String search = params.get("search");
        String genre = params.get("genre");
        String type = params.get("type");
        String year = params.get("year");
        String minYear = params.get("minYear");
        String maxYear = params.get("maxYear");
        String maxAge = params.get("maxAge");
        String sortBy = params.get("sortBy");
        String sortOrder = params.get("sortOrder");

        List<MediaEntry> results;

        if (genre != null || type != null || year != null || minYear != null ||
                maxYear != null || maxAge != null || sortBy != null) {
            Map<String, String> filters = new HashMap<>();
            if (genre != null) filters.put("genre", genre);
            if (type != null) filters.put("type", type);
            if (year != null) filters.put("year", year);
            if (minYear != null) filters.put("minYear", minYear);
            if (maxYear != null) filters.put("maxYear", maxYear);
            if (maxAge != null) filters.put("maxAge", maxAge);
            results = mediaService.filterAndSort(filters, sortBy, sortOrder);
        } else if (search != null) {
            results = mediaService.searchByTitle(search);
        } else {
            results = mediaService.list(null);
        }

        sendResponse(exchange, 200, results);
    }

    private void handleGet(HttpExchange exchange, String idStr) throws IOException, SQLException {
        try {
            UUID id = UUID.fromString(idStr);
            Optional<MediaEntry> media = mediaService.get(id);

            if (media.isEmpty()) {
                sendResponse(exchange, 404, Map.of("error", "Media not found"));
                return;
            }

            MediaEntry entry = media.get();
            Map<String, Object> response = Map.of(
                    "id", entry.getId(),
                    "ownerId", entry.getOwnerId(),
                    "title", entry.getTitle(),
                    "description", entry.getDescription(),
                    "mediaType", entry.getMediaType(),
                    "releaseYear", entry.getReleaseYear() != null ? entry.getReleaseYear() : 0,
                    "genres", entry.getGenres() != null ? entry.getGenres() : "",
                    "ageRestriction", entry.getAgeRestriction() != null ? entry.getAgeRestriction() : 0,
                    "averageScore", mediaService.averageScore(id)
            );

            sendResponse(exchange, 200, response);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, Map.of("error", "Invalid media ID"));
        }
    }

    private void handleUpdate(HttpExchange exchange, String path) throws IOException, SQLException {
        Optional<User> userOpt = authenticateUser(exchange);
        if (userOpt.isEmpty()) {
            sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
            return;
        }

        String[] parts = path.split("/");
        if (parts.length < 4) {  // Changed from 3 to 4
            sendResponse(exchange, 400, Map.of("error", "Media ID required"));
            return;
        }

        UUID mediaId = UUID.fromString(parts[3]);  // Changed from parts[2] to parts[3]
        Map<String, Object> body = parseRequestBody(exchange);
        mediaService.updateFromRequest(mediaId, userOpt.get(), body);

        sendResponse(exchange, 200, Map.of("message", "Media updated successfully"));
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException, SQLException {
        Optional<User> userOpt = authenticateUser(exchange);
        if (userOpt.isEmpty()) {
            sendResponse(exchange, 401, Map.of("error", "Unauthorized"));
            return;
        }

        String[] parts = path.split("/");
        if (parts.length < 4) {  // Changed from 3 to 4
            sendResponse(exchange, 400, Map.of("error", "Media ID required"));
            return;
        }

        UUID mediaId = UUID.fromString(parts[3]);  // Changed from parts[2] to parts[3]
        mediaService.delete(mediaId, userOpt.get());

        sendResponse(exchange, 200, Map.of("message", "Media deleted successfully"));
    }
}
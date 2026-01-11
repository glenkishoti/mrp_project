package at.fhtw.mrp.http;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.UserRepository;
import at.fhtw.mrp.service.FavoriteService;
import at.fhtw.mrp.util.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.*;

/**
 * HTTP Handler for Favorite endpoints
 *
 * Endpoints:
 * - POST   /api/favorites              → Add favorite
 * - DELETE /api/favorites/{mediaId}    → Remove favorite
 * - GET    /api/favorites              → Get user's favorites
 * - GET    /api/favorites/{mediaId}/status → Check if favorited
 */
public class FavoriteHandler implements HttpHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final FavoriteService favoriteService;
    private final UserRepository userRepo;

    public FavoriteHandler(FavoriteService favoriteService, UserRepository userRepo) {
        this.favoriteService = favoriteService;
        this.userRepo = userRepo;
    }

    @Override
    public void handle(HttpExchange ex) {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            // Handle CORS preflight
            if (method.equals("OPTIONS")) {
                send(ex, 200, Map.of());
                return;
            }

            // GET /api/favorites (list user's favorites)
            if (method.equals("GET") && path.equals("/api/favorites")) {
                User user = requireAuth(ex);
                List<MediaEntry> favorites = favoriteService.getUserFavorites(user.getId());
                send(ex, 200, favorites);
                return;
            }

            // POST /api/favorites (add favorite)
            if (method.equals("POST") && path.equals("/api/favorites")) {
                User user = requireAuth(ex);
                Map<String, Object> body = readJson(ex);

                String mediaIdStr = (String) body.get("mediaId");
                if (mediaIdStr == null || mediaIdStr.isBlank()) {
                    send(ex, 400, Map.of("error", "mediaId is required"));
                    return;
                }

                UUID mediaId = UUID.fromString(mediaIdStr);
                favoriteService.addFavorite(user.getId(), mediaId);

                send(ex, 201, Map.of("message", "Added to favorites"));
                return;
            }

            // DELETE /api/favorites/{mediaId} (remove favorite)
            if (method.equals("DELETE") && path.matches("^/api/favorites/.+$")) {
                User user = requireAuth(ex);
                UUID mediaId = extractUUID(path, "/api/favorites/");

                if (mediaId == null) {
                    send(ex, 400, Map.of("error", "Invalid media ID"));
                    return;
                }

                favoriteService.removeFavorite(user.getId(), mediaId);
                send(ex, 200, Map.of("message", "Removed from favorites"));
                return;
            }

            // GET /api/favorites/{mediaId}/status (check if favorited)
            if (method.equals("GET") && path.matches("^/api/favorites/.+/status$")) {
                User user = requireAuth(ex);

                // Extract mediaId from path: /api/favorites/{mediaId}/status
                String pathWithoutStatus = path.replace("/status", "");
                UUID mediaId = extractUUID(pathWithoutStatus, "/api/favorites/");

                if (mediaId == null) {
                    send(ex, 400, Map.of("error", "Invalid media ID"));
                    return;
                }

                boolean isFavorite = favoriteService.isFavorite(user.getId(), mediaId);
                send(ex, 200, Map.of("isFavorite", isFavorite));
                return;
            }

            // No route matched
            send(ex, 404, Map.of("error", "Not found"));

        } catch (SecurityException se) {
            send(ex, 401, Map.of("error", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            send(ex, 400, Map.of("error", iae.getMessage()));
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            send(ex, 500, Map.of("error", "Database error"));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, Map.of("error", "Server error"));
        }
    }

    /**
     * Require authentication - throw SecurityException if not authenticated
     */
    private User requireAuth(HttpExchange ex) throws Exception {
        String authHeader = ex.getRequestHeaders().getFirst("Authorization");
        Optional<User> userOpt = TokenService.authenticate(authHeader, userRepo);
        if (userOpt.isEmpty()) {
            throw new SecurityException("Unauthorized");
        }
        return userOpt.get();
    }

    /**
     * Read JSON request body
     */
    private Map<String, Object> readJson(HttpExchange ex) throws Exception {
        try (InputStream in = ex.getRequestBody()) {
            if (in == null) return Map.of();
            return mapper.readValue(in, Map.class);
        }
    }

    /**
     * Send JSON response
     */
    private void send(HttpExchange ex, int code, Object payload) {
        try {
            byte[] bytes = (payload instanceof byte[])
                    ? (byte[]) payload
                    : mapper.writeValueAsBytes(payload);

            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(code, bytes.length);

            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract UUID from path
     */
    private UUID extractUUID(String path, String prefix) {
        if (!path.startsWith(prefix)) return null;
        String idStr = path.substring(prefix.length());

        if (idStr.endsWith("/")) {
            idStr = idStr.substring(0, idStr.length() - 1);
        }

        if (!idStr.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return null;
        }

        try {
            return UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
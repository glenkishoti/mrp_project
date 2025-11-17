package at.fhtw.mrp.http;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.IUserRepository;
import at.fhtw.mrp.service.IAuthService;
import at.fhtw.mrp.service.IMediaService;
import at.fhtw.mrp.service.IRatingService;
import at.fhtw.mrp.util.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class MediaHandler implements HttpHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IMediaService mediaService;
    private final IRatingService ratingService;
    private final IAuthService authService;

    public MediaHandler(IMediaService mediaService,
                        IRatingService ratingService,
                        IAuthService authService) {
        this.mediaService = mediaService;
        this.ratingService = ratingService;
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange ex) {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            if (method.equals("OPTIONS")) {
                send(ex, 200, Map.of());
                return;
            }

            // -----------------------------------------------------------
            // GET /api/media?query=text
            // -----------------------------------------------------------
            if (method.equals("GET") && path.equals("/api/media")) {
                String query = ex.getRequestURI().getQuery();
                String search = parseQuery(query).get("query");
                send(ex, 200, mediaService.list(search));
                return;
            }

            // -----------------------------------------------------------
            // POST /api/media   (auth required)
            // -----------------------------------------------------------
            if (method.equals("POST") && path.equals("/api/media")) {
                User user = requireAuth(ex);
                Map<String, Object> body = readJson(ex);

                UUID id = mediaService.createFromRequest(user, body);
                send(ex, 201, Map.of("id", id.toString()));
                return;
            }

            // -----------------------------------------------------------
            // POST /api/media/{id}/ratings
            // -----------------------------------------------------------
            if (method.equals("POST") && path.matches("^/api/media/.+/ratings$")) {
                User user = requireAuth(ex);
                UUID mediaId = extractUUID(path, "/api/media/", "/ratings");

                Map<String, Object> body = readJson(ex);
                int stars = ((Number) body.get("stars")).intValue();
                String comment = (String) body.get("comment");

                UUID ratingId = ratingService.create(mediaId, user.getId(), stars, comment);
                send(ex, 201, Map.of("id", ratingId.toString()));
                return;
            }

            // -----------------------------------------------------------
            // GET /api/media/{id}/ratings
            // -----------------------------------------------------------
            if (method.equals("GET") && path.matches("^/api/media/.+/ratings$")) {
                UUID mediaId = extractUUID(path, "/api/media/", "/ratings");
                send(ex, 200, ratingService.listByMedia(mediaId));
                return;
            }

            // -----------------------------------------------------------
            // /api/media/{id} - individual media operations
            // -----------------------------------------------------------
            UUID mediaId = extractUUID(path, "/api/media/");
            if (mediaId != null) {

                // GET /api/media/{id}
                if (method.equals("GET")) {
                    Optional<MediaEntry> entryOpt = mediaService.get(mediaId);
                    if (entryOpt.isEmpty()) {
                        send(ex, 404, Map.of("error", "Not found"));
                        return;
                    }

                    MediaEntry entry = entryOpt.get();
                    double avg = mediaService.averageScore(mediaId);

                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", entry.getId().toString());
                    payload.put("ownerId", entry.getOwnerId().toString());
                    payload.put("title", entry.getTitle());
                    payload.put("description", entry.getDescription());
                    payload.put("mediaType", entry.getMediaType());
                    payload.put("releaseYear", entry.getReleaseYear());
                    payload.put("genres", entry.getGenres());
                    payload.put("ageRestriction", entry.getAgeRestriction());
                    payload.put("averageScore", avg);

                    send(ex, 200, payload);
                    return;
                }

                // PUT /api/media/{id}
                if (method.equals("PUT")) {
                    User user = requireAuth(ex);
                    Map<String, Object> body = readJson(ex);

                    mediaService.updateFromRequest(mediaId, user, body);
                    send(ex, 200, Map.of("message", "updated"));
                    return;
                }

                // DELETE /api/media/{id}
                if (method.equals("DELETE")) {
                    User user = requireAuth(ex);
                    mediaService.delete(mediaId, user);
                    send(ex, 204, new byte[0]);
                    return;
                }
            }

            send(ex, 404, Map.of("error", "Not found"));

        } catch (SecurityException se) {
            send(ex, 401, Map.of("error", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            send(ex, 400, Map.of("error", iae.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, Map.of("error", "Server error"));
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private User requireAuth(HttpExchange ex) throws Exception {
        String authHeader = ex.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Unauthorized");
        }

        // For now, we'll use a simple approach - you may need to adjust this
        // based on your actual authentication implementation
        String token = authHeader.substring("Bearer ".length()).trim();

        // Parse userId from token (assuming format: "userId;username;secret")
        String[] parts = token.split(";");
        if (parts.length < 2) {
            throw new SecurityException("Invalid token");
        }

        UUID userId = UUID.fromString(parts[0]);
        String username = parts[1];

        // Create a minimal User object for authorization purposes
        // In a real implementation, you'd fetch this from the repository
        return new User(userId, username, null, token);
    }

    private Map<String, Object> readJson(HttpExchange ex) throws Exception {
        try (InputStream in = ex.getRequestBody()) {
            if (in == null) return Map.of();
            return mapper.readValue(in, Map.class);
        }
    }

    private void send(HttpExchange ex, int code, Object payload) {
        try {
            byte[] json = (payload instanceof byte[])
                    ? (byte[]) payload
                    : mapper.writeValueAsBytes(payload);

            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(code, json.length);

            try (OutputStream os = ex.getResponseBody()) {
                os.write(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> parseQuery(String q) {
        Map<String, String> m = new HashMap<>();
        if (q == null) return m;

        for (String kv : q.split("&")) {
            String[] p = kv.split("=", 2);
            m.put(p[0], p.length == 2 ? p[1] : "");
        }
        return m;
    }

    private UUID extractUUID(String path, String prefix) {
        if (!path.startsWith(prefix)) return null;
        String idStr = path.substring(prefix.length());

        // Handle trailing slash
        if (idStr.endsWith("/")) {
            idStr = idStr.substring(0, idStr.length() - 1);
        }

        // Basic UUID format check
        if (!idStr.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return null;
        }

        try {
            return UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private UUID extractUUID(String path, String prefix, String suffix) {
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) return null;
        String part = path.substring(prefix.length(), path.length() - suffix.length());

        try {
            return UUID.fromString(part);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
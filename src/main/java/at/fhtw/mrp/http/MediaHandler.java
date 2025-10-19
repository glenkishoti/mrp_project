package at.fhtw.mrp.http;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.MediaRepository;
import at.fhtw.mrp.repo.RatingRepository;
import at.fhtw.mrp.repo.UserRepository;
import at.fhtw.mrp.service.MediaService;
import at.fhtw.mrp.service.RatingService;
import at.fhtw.mrp.util.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

/**
 * Handles all routes under /api/media
 * - GET    /api/media?query=term
 * - POST   /api/media                                   (auth)
 * - GET    /api/media/{mediaId}
 * - PUT    /api/media/{mediaId}                         (owner only)
 * - DELETE /api/media/{mediaId}                         (owner only)
 * - POST   /api/media/{mediaId}/ratings                 (auth)
 * - GET    /api/media/{mediaId}/ratings
 */
public class MediaHandler implements HttpHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final MediaService media = new MediaService(new MediaRepository());
    private final RatingService ratings = new RatingService(new RatingRepository());
    private final UserRepository users = new UserRepository();

    @Override
    public void handle(HttpExchange ex) {
        try {
            final String method = ex.getRequestMethod();
            final URI uri = ex.getRequestURI();
            final String path = uri.getPath();     // /api/media or /api/media/{id}[...]
            final String query = uri.getQuery();   // query=...

            if ("OPTIONS".equalsIgnoreCase(method)) { send(ex, 200, Map.of()); return; }

            // LIST: GET /api/media?query=...
            if ("GET".equalsIgnoreCase(method) && "/api/media".equals(path)) {
                String q = parseQuery(query).get("query");
                send(ex, 200, media.list(q));
                return;
            }

            // CREATE: POST /api/media (auth)
            if ("POST".equalsIgnoreCase(method) && "/api/media".equals(path)) {
                User me = requireAuth(ex);
                Map<String, Object> body = readJson(ex);
                String title = (String) body.get("title");
                String description = (String) body.get("description");
                String type = (String) body.get("mediaType");
                Integer year = toInt(body.get("releaseYear"));
                String genres = (String) body.get("genres");
                Integer age = toInt(body.get("ageRestriction"));

                int createdMediaId = media.create(me.getId(), title, description, type, year, genres, age);
                send(ex, 201, Map.of("id", createdMediaId));
                return;
            }

            // RATINGS (under /api/media/{id})
            // POST /api/media/{id}/ratings (create)
            if ("POST".equalsIgnoreCase(method) && path.matches("^/api/media/\\d+/ratings$")) {
                User me = requireAuth(ex);
                int mediaId = Integer.parseInt(path.split("/")[3]); // /api/media/{id}/ratings
                Map<String, Object> body = readJson(ex);
                int stars = Integer.parseInt(String.valueOf(body.get("stars")));
                String comment = (String) body.get("comment");
                int createdRatingId = ratings.create(mediaId, me.getId(), stars, comment);
                send(ex, 201, Map.of("id", createdRatingId));
                return;
            }

            // GET /api/media/{id}/ratings (list)
            if ("GET".equalsIgnoreCase(method) && path.matches("^/api/media/\\d+/ratings$")) {
                int mediaId = Integer.parseInt(path.split("/")[3]);
                send(ex, 200, ratings.listByMedia(mediaId));
                return;
            }

            // /api/media/{id}
            Integer mediaPathId = extractId(path, "/api/media/");
            if (mediaPathId != null) {

                // GET one
                if ("GET".equalsIgnoreCase(method)) {
                    var opt = media.get(mediaPathId);
                    if (opt.isEmpty()) { send(ex, 404, Map.of("error", "Not found")); return; }
                    MediaEntry entry = opt.get();
                    Double avg = media.averageScore(mediaPathId);

                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", entry.getId());
                    payload.put("ownerId", entry.getOwnerId());
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

                // UPDATE (owner)
                if ("PUT".equalsIgnoreCase(method)) {
                    User me = requireAuth(ex);
                    Map<String, Object> body = readJson(ex);
                    media.update(
                            mediaPathId,
                            me.getId(),
                            (String) body.get("title"),
                            (String) body.get("description"),
                            (String) body.get("mediaType"),
                            toInt(body.get("releaseYear")),
                            (String) body.get("genres"),
                            toInt(body.get("ageRestriction"))
                    );
                    send(ex, 200, Map.of("message", "updated"));
                    return;
                }

                // DELETE (owner)
                if ("DELETE".equalsIgnoreCase(method)) {
                    User me = requireAuth(ex);
                    media.delete(mediaPathId, me.getId());
                    send(ex, 204, new byte[0]);
                    return;
                }
            }

            send(ex, 404, Map.of("error", "Not found"));

        } catch (SecurityException se) {
            send(ex, 401, Map.of("error", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            send(ex, 400, Map.of("error", iae.getMessage()));
        } catch (SQLException sqle) {
            send(ex, 500, Map.of("error", "DB error"));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, Map.of("error", "Server error"));
        }
    }

    // ---------- helpers ----------

    private Map<String, Object> readJson(HttpExchange ex) throws Exception {
        try (InputStream in = ex.getRequestBody()) {
            if (in == null) return Map.of();
            return mapper.readValue(in, Map.class);
        }
    }

    private Map<String, String> parseQuery(String q) {
        Map<String, String> m = new HashMap<>();
        if (q == null || q.isBlank()) return m;
        for (String kv : q.split("&")) {
            String[] p = kv.split("=", 2);
            m.put(p[0], p.length == 2 ? p[1] : "");
        }
        return m;
    }

    /** Extracts a numeric id if path starts with prefix and has only the id after it. */
    private Integer extractId(String path, String prefix) {
        if (!path.startsWith(prefix)) return null;
        String rest = path.substring(prefix.length());
        if (!rest.matches("\\d+")) return null;
        return Integer.parseInt(rest);
    }

    private Integer toInt(Object o) { return (o == null) ? null : Integer.valueOf(String.valueOf(o)); }

    private User requireAuth(HttpExchange ex) throws Exception {
        String authz = ex.getRequestHeaders().getFirst("Authorization");
        var userOpt = TokenService.authenticate(authz, users);
        if (userOpt.isEmpty()) throw new SecurityException("Unauthorized");
        return userOpt.get();
    }

    private void send(HttpExchange ex, int code, Object payload) {
        try {
            byte[] bytes = (payload instanceof byte[]) ? (byte[]) payload
                    : mapper.writeValueAsBytes(payload);
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        } catch (Exception ignored) {}
    }
}

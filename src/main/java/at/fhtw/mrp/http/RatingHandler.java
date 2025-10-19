package at.fhtw.mrp.http;

import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.RatingRepository;
import at.fhtw.mrp.repo.UserRepository;
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

public class RatingHandler implements HttpHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RatingService ratings = new RatingService(new RatingRepository());
    private final UserRepository users = new UserRepository();

    @Override public void handle(HttpExchange ex) {
        try {
            String method = ex.getRequestMethod();
            URI uri = ex.getRequestURI();
            String path = uri.getPath();
            if (method.equals("OPTIONS")) { send(ex,200, Map.of()); return; }

            // POST /api/media/{id}/ratings   (create)
            if (method.equalsIgnoreCase("POST") && path.matches("^/api/media/\\d+/ratings$")) {
                User me = requireAuth(ex);
                int mediaId = Integer.parseInt(path.split("/")[3]);
                Map<String,Object> body = readJson(ex);
                int stars = Integer.parseInt(String.valueOf(body.get("stars")));
                String comment = (String) body.get("comment");
                int id = ratings.create(mediaId, me.getId(), stars, comment);
                send(ex, 201, Map.of("id", id));
                return;
            }

            // GET /api/media/{id}/ratings    (list)
            if (method.equalsIgnoreCase("GET") && path.matches("^/api/media/\\d+/ratings$")) {
                int mediaId = Integer.parseInt(path.split("/")[3]);
                send(ex, 200, ratings.listByMedia(mediaId));
                return;
            }

            // PUT /api/ratings/{ratingId}    (update own)
            if (method.equalsIgnoreCase("PUT") && path.matches("^/api/ratings/\\d+$")) {
                User me = requireAuth(ex);
                int ratingId = Integer.parseInt(path.substring("/api/ratings/".length()));
                Map<String,Object> body = readJson(ex);
                int stars = Integer.parseInt(String.valueOf(body.get("stars")));
                String comment = (String) body.get("comment");
                Boolean confirm = (body.containsKey("confirm") ? Boolean.valueOf(String.valueOf(body.get("confirm"))) : null);
                ratings.update(ratingId, me.getId(), stars, comment, confirm);
                send(ex, 200, Map.of("message","updated"));
                return;
            }

            // DELETE /api/ratings/{ratingId} (delete own)
            if (method.equalsIgnoreCase("DELETE") && path.matches("^/api/ratings/\\d+$")) {
                User me = requireAuth(ex);
                int ratingId = Integer.parseInt(path.substring("/api/ratings/".length()));
                ratings.delete(ratingId, me.getId());
                send(ex, 204, new byte[0]);
                return;
            }

            send(ex, 404, Map.of("error","Not found"));
        } catch (SecurityException se) {
            send(ex, 401, Map.of("error", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            send(ex, 400, Map.of("error", iae.getMessage()));
        } catch (SQLException sqle) {
            send(ex, 500, Map.of("error","DB error"));
        } catch (Exception e) {
            e.printStackTrace(); send(ex, 500, Map.of("error","Server error"));
        }
    }

    // helpers
    private Map<String,Object> readJson(HttpExchange ex) throws Exception {
        try (InputStream in = ex.getRequestBody()) {
            if (in == null) return Map.of();
            return mapper.readValue(in, Map.class);
        }
    }
    private at.fhtw.mrp.model.User requireAuth(HttpExchange ex) throws Exception {
        var authz = ex.getRequestHeaders().getFirst("Authorization");
        var opt = TokenService.authenticate(authz, users);
        if (opt.isEmpty()) throw new SecurityException("Unauthorized");
        return opt.get();
    }
    private void send(HttpExchange ex, int code, Object payload) {
        try {
            byte[] bytes = (payload instanceof byte[]) ? (byte[]) payload : mapper.writeValueAsBytes(payload);
            ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
            ex.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        } catch (Exception ignored) {}
    }
}

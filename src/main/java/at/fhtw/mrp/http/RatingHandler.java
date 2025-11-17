package at.fhtw.mrp.http;

import at.fhtw.mrp.model.Rating;
import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.IUserRepository;
import at.fhtw.mrp.service.IAuthService;
import at.fhtw.mrp.service.IRatingService;
import at.fhtw.mrp.util.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.*;

public class RatingHandler implements HttpHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IRatingService ratingService;
    private final IAuthService authService;
    private final IUserRepository userRepo;

    public RatingHandler(IRatingService ratingService,
                         IAuthService authService,
                         IUserRepository userRepo) {
        this.ratingService = ratingService;
        this.authService = authService;
        this.userRepo = userRepo;
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
            // DELETE /api/ratings/{ratingId}
            // -----------------------------------------------------------
            if (method.equals("DELETE") && path.matches("^/api/ratings/.+$")) {
                User user = requireAuth(ex);
                UUID ratingId = extractUUID(path, "/api/ratings/");

                if (ratingId == null) {
                    send(ex, 400, Map.of("error", "Invalid rating ID"));
                    return;
                }

                // Service handles ownership verification
                ratingService.delete(ratingId, user.getId());
                send(ex, 204, new byte[0]);
                return;
            }

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

    // -------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------

    private User requireAuth(HttpExchange ex) throws Exception {
        String authHeader = ex.getRequestHeaders().getFirst("Authorization");
        Optional<User> userOpt = TokenService.authenticate(authHeader, userRepo);
        if (userOpt.isEmpty()) {
            throw new SecurityException("Unauthorized");
        }
        return userOpt.get();
    }

    private Map<String, Object> readJson(HttpExchange ex) throws Exception {
        try (InputStream in = ex.getRequestBody()) {
            if (in == null) return Map.of();
            return mapper.readValue(in, Map.class);
        }
    }

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
}
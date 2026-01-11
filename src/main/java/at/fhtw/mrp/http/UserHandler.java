package at.fhtw.mrp.http;

import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.UserRepository;
import at.fhtw.mrp.service.AuthService;
import at.fhtw.mrp.util.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP Handler for User endpoints
 *
 * Endpoints:
 * - POST /api/users/register
 * - POST /api/users/login
 * - GET  /api/users/{username}/profile
 */
public class UserHandler implements HttpHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthService authService;
    private final UserRepository userRepo;

    public UserHandler(AuthService authService, UserRepository userRepo) {
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

            // POST /api/users/register
            if (method.equals("POST") && path.equals("/api/users/register")) {
                Map<String, Object> body = readJson(ex);
                String username = (String) body.get("username");
                String password = (String) body.get("password");

                authService.register(username, password);
                send(ex, 201, Map.of("message", "User registered successfully"));
                return;
            }

            // POST /api/users/login
            if (method.equals("POST") && path.equals("/api/users/login")) {
                Map<String, Object> body = readJson(ex);
                String username = (String) body.get("Username");
                String password = (String) body.get("Password");

                String token = authService.authenticate(username, password);
                send(ex, 200, Map.of("token", token));
                return;
            }

            // GET /api/users/{username}/profile
            if (method.equals("GET") && path.matches("^/api/users/.+/profile$")) {
                User user = requireAuth(ex);

                // Extract username from path
                String[] parts = path.split("/");
                String requestedUsername = parts[3];

                // Check if user is requesting their own profile
                if (!user.getUsername().equals(requestedUsername)) {
                    send(ex, 403, Map.of("error", "Forbidden"));
                    return;
                }

                Map<String, Object> profile = Map.of(
                        "id", user.getId().toString(),
                        "username", user.getUsername()
                );
                send(ex, 200, profile);
                return;
            }

            send(ex, 404, Map.of("error", "Not found"));

        } catch (SecurityException se) {
            send(ex, 401, Map.of("error", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            send(ex, 400, Map.of("error", iae.getMessage()));
        } catch (SQLException sqle) {
            send(ex, 500, Map.of("error", "Database error"));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, Map.of("error", "Server error"));
        }
    }

    private Map<String, Object> readJson(HttpExchange ex) throws Exception {
        try (InputStream in = ex.getRequestBody()) {
            if (in == null) return Map.of();
            return mapper.readValue(in, Map.class);
        }
    }

    private User requireAuth(HttpExchange ex) throws Exception {
        String authHeader = ex.getRequestHeaders().getFirst("Authorization");
        Optional<User> userOpt = TokenService.authenticate(authHeader, userRepo);
        if (userOpt.isEmpty()) {
            throw new SecurityException("Unauthorized");
        }
        return userOpt.get();
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
}
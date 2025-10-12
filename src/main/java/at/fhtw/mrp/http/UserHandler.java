package at.fhtw.mrp.http;

import at.fhtw.mrp.repo.UserRepository;
import at.fhtw.mrp.service.AuthService;
import at.fhtw.mrp.util.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

public class UserHandler implements HttpHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthService auth = new AuthService(new UserRepository());

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            URI uri = ex.getRequestURI();
            String path = uri.getPath(); // e.g. /api/users/register

            if (method.equalsIgnoreCase("POST") && path.endsWith("/register")) {
                handleRegister(ex);
                return;
            }
            if (method.equalsIgnoreCase("POST") && path.endsWith("/login")) {
                handleLogin(ex);
                return;
            }
            // GET /api/users/{username}/profile
            if (method.equalsIgnoreCase("GET") && path.matches(".*/api/users/[^/]+/profile$")) {
                handleProfile(ex);
                return;
            }

            send(ex, 404, Map.of("error", "Not found"));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    private void handleRegister(HttpExchange ex) throws IOException {
        Map<String, Object> body = jsonBody(ex);
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        try {
            auth.register(username, password);
            send(ex, 201, Map.of("message", "User registered"));
        } catch (IllegalArgumentException e) {
            send(ex, 400, Map.of("error", e.getMessage()));
        } catch (SQLException e) {
            send(ex, 500, Map.of("error", "DB error"));
        }
    }

    // spec shows capitalized keys "Username"/"Password"
    private void handleLogin(HttpExchange ex) throws IOException {
        Map<String, Object> body = jsonBody(ex);
        String username = asString(body.getOrDefault("Username", body.get("username")));
        String password = asString(body.getOrDefault("Password", body.get("password")));
        try {
            Map<String, String> tokenMap = auth.login(username, password);
            send(ex, 200, tokenMap);
        } catch (IllegalArgumentException e) {
            send(ex, 401, Map.of("error", e.getMessage()));
        } catch (SQLException e) {
            send(ex, 500, Map.of("error", "DB error"));
        }
    }

    private void handleProfile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");
        String username = parts[parts.length - 2]; // .../users/{username}/profile

        String authz = ex.getRequestHeaders().getFirst("Authorization");
        try {
            var userOpt = TokenService.authenticate(authz, new UserRepository());
            if (userOpt.isEmpty()) {
                send(ex, 401, Map.of("error", "Unauthorized"));
                return;
            }
            var user = userOpt.get();
            if (!user.getUsername().equals(username)) {
                send(ex, 403, Map.of("error", "Forbidden"));
                return;
            }
            // Minimal example profile payload
            send(ex, 200, Map.of(
                    "username", user.getUsername(),
                    "id", user.getId(),
                    "hasToken", user.getToken() != null
            ));
        } catch (SQLException e) {
            send(ex, 500, Map.of("error", "DB error"));
        }
    }

    private Map<String, Object> jsonBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            if (in == null) return Map.of();
            return mapper.readValue(in, Map.class);
        }
    }

    private void send(HttpExchange ex, int code, Object payload) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(payload);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}

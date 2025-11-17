package at.fhtw.mrp.model;

import java.util.UUID;

public class User {
    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final String token;   // nullable

    public User(UUID id, String username, String passwordHash, String token) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.token = token;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getToken() {
        return token;
    }

    public User withToken(String newToken) {
        return new User(id, username, passwordHash, newToken);
    }
}

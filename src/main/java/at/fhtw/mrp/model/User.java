package at.fhtw.mrp.model;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String token;

    public User() {}

    public User(int id, String username, String passwordHash, String token) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.token = token;
    }

    // getters and setters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getToken() { return token; }

    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setToken(String token) { this.token = token; }
}

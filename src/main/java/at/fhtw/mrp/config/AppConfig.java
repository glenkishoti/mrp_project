package at.fhtw.mrp.config;

public final class AppConfig {
    public static final String DB_HOST  = System.getenv().getOrDefault("DB_HOST", "localhost");
    public static final String DB_PORT  = System.getenv().getOrDefault("DB_PORT", "5432");
    public static final String DB_NAME  = System.getenv().getOrDefault("DB_NAME", "mrp_db");
    public static final String DB_USER  = System.getenv().getOrDefault("DB_USER", "mrp_user");
    public static final String DB_PASS  = System.getenv().getOrDefault("DB_PASS", "mrp_password");

    public static String jdbcUrl() {
        return "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    }
}

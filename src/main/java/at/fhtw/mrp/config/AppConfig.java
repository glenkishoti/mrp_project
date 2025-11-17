package at.fhtw.mrp.config;

/**
 * Centralized application configuration
 * All configuration is read from environment variables with sensible defaults
 */
public final class AppConfig {

    // Private constructor - utility class, no instances
    private AppConfig() {
        throw new AssertionError("Cannot instantiate AppConfig");
    }

    // Database configuration
    public static final String DB_HOST = System.getenv().getOrDefault("MRP_DB_HOST", "localhost");
    public static final String DB_PORT = System.getenv().getOrDefault("MRP_DB_PORT", "5432");
    public static final String DB_NAME = System.getenv().getOrDefault("MRP_DB_NAME", "mrp_db");
    public static final String DB_USER = System.getenv().getOrDefault("MRP_DB_USER", "mrp_user");
    public static final String DB_PASS = System.getenv().getOrDefault("MRP_DB_PASS", "mrp_password");

    /**
     * Build complete JDBC connection URL
     * @return JDBC URL string for PostgreSQL
     */
    public static String getJdbcUrl() {
        return "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    }

    /**
     * Get database username
     * @return database username
     */
    public static String getDbUser() {
        return DB_USER;
    }

    /**
     * Get database password
     * @return database password
     */
    public static String getDbPassword() {
        return DB_PASS;
    }
}
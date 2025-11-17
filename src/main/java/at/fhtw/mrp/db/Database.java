package at.fhtw.mrp.db;

import at.fhtw.mrp.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection utility
 * Uses configuration from AppConfig
 */
public final class Database {

    // Private constructor - utility class, no instances
    private Database() {
        throw new AssertionError("Cannot instantiate Database");
    }

    /**
     * Get a new database connection
     * Connection parameters are read from AppConfig
     *
     * @return Connection object connected to PostgreSQL
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.getJdbcUrl(),      // jdbc:postgresql://localhost:5432/mrp_db
                AppConfig.getDbUser(),        // mrp_user
                AppConfig.getDbPassword()     // mrp_password
        );
    }
}
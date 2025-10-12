package at.fhtw.mrp.db;

import at.fhtw.mrp.config.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.jdbcUrl(),
                AppConfig.DB_USER,
                AppConfig.DB_PASS
        );
    }
}

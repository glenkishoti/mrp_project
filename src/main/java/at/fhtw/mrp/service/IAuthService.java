package at.fhtw.mrp.service;

import java.sql.SQLException;
import java.util.UUID;

public interface IAuthService {
    UUID register(String username, String password) throws SQLException;
    String login(String username, String password) throws SQLException;
}

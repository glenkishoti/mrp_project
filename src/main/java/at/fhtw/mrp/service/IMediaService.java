package at.fhtw.mrp.service;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IMediaService {

    UUID createFromRequest(User owner, Map<String, Object> body) throws SQLException;

    Optional<MediaEntry> get(UUID id) throws SQLException;

    List<MediaEntry> list(String query) throws SQLException;

    double averageScore(UUID mediaId) throws SQLException;

    void updateFromRequest(UUID mediaId, User owner, Map<String, Object> body) throws SQLException;

    void delete(UUID mediaId, User owner) throws SQLException;
}

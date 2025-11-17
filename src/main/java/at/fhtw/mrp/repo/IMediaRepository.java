package at.fhtw.mrp.repo;

import at.fhtw.mrp.model.MediaEntry;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMediaRepository {

    void insert(MediaEntry entry) throws SQLException;

    Optional<MediaEntry> findById(UUID id) throws SQLException;

    List<MediaEntry> list(String query) throws SQLException;

    void update(MediaEntry entry) throws SQLException;

    void delete(UUID mediaId) throws SQLException;

    double averageScore(UUID mediaId) throws SQLException;
}

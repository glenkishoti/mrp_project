package at.fhtw.mrp.repo;

import at.fhtw.mrp.model.Rating;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRatingRepository {

    void insert(Rating rating) throws SQLException;

    Optional<Rating> findById(UUID id) throws SQLException;

    List<Rating> listByMedia(UUID mediaId) throws SQLException;

    List<Rating> listByUser(UUID userId) throws SQLException;

    void delete(UUID ratingId) throws SQLException;
}
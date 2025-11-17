package at.fhtw.mrp.service;

import at.fhtw.mrp.model.Rating;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRatingService {

    UUID create(UUID mediaId, UUID userId, int stars, String comment) throws SQLException;

    List<Rating> listByMedia(UUID mediaId) throws SQLException;

    List<Rating> listByUser(UUID userId) throws SQLException;

    Optional<Rating> findById(UUID ratingId) throws SQLException;

    void delete(UUID ratingId, UUID userId) throws SQLException;
}
package at.fhtw.mrp.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Single service interface with template methods for all business logic
 * All services (Auth, Media, Rating, Favorite) implement this interface
 */
public interface IService {

    // ===== CREATION OPERATIONS =====

    /**
     * Create a new entity from request data
     * @param userId - the user creating the entity (for authorization)
     * @param data - request data as Map
     * @return UUID of created entity
     * @throws SQLException if database error occurs
     * @throws IllegalArgumentException if validation fails
     */
    UUID create(UUID userId, Map<String, Object> data) throws SQLException;

    // ===== RETRIEVAL OPERATIONS =====

    /**
     * Get an entity by its ID
     * @param id - the entity's UUID
     * @return Optional containing entity if found, empty otherwise
     * @throws SQLException if database error occurs
     */
    Optional<?> get(UUID id) throws SQLException;

    /**
     * List all entities (optionally filtered by query)
     * @param query - optional search/filter parameter
     * @return List of entities
     * @throws SQLException if database error occurs
     */
    List<?> list(String query) throws SQLException;

    /**
     * List entities related to a specific user
     * @param userId - the user's UUID
     * @return List of user's entities
     * @throws SQLException if database error occurs
     */
    List<?> listByUser(UUID userId) throws SQLException;

    // ===== UPDATE OPERATIONS =====

    /**
     * Update an existing entity
     * @param id - the entity's UUID
     * @param userId - the user requesting update (for authorization)
     * @param data - updated data as Map
     * @throws SQLException if database error occurs
     * @throws SecurityException if user not authorized
     * @throws IllegalArgumentException if entity not found
     */
    void update(UUID id, UUID userId, Map<String, Object> data) throws SQLException;

    // ===== DELETE OPERATIONS =====

    /**
     * Delete an entity
     * @param id - the entity's UUID
     * @param userId - the user requesting deletion (for authorization)
     * @throws SQLException if database error occurs
     * @throws SecurityException if user not authorized
     * @throws IllegalArgumentException if entity not found
     */
    void delete(UUID id, UUID userId) throws SQLException;

    // ===== AUTHENTICATION/AUTHORIZATION =====

    /**
     * Authenticate user with credentials
     * @param username - username
     * @param password - password
     * @return authentication token
     * @throws SQLException if database error occurs
     * @throws IllegalArgumentException if credentials invalid
     */
    String authenticate(String username, String password) throws SQLException;

    /**
     * Register new user
     * @param username - desired username
     * @param password - password
     * @return UUID of created user
     * @throws SQLException if database error occurs
     * @throws IllegalArgumentException if username taken
     */
    UUID register(String username, String password) throws SQLException;
}
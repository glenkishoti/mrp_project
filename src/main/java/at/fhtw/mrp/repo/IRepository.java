package at.fhtw.mrp.repo;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Single repository interface with template methods for all data access
 * All repositories (User, Media, Rating, Favorite) implement this interface
 *
 * Note: Not all methods are applicable to all entity types.
 * Implementations should throw UnsupportedOperationException for inapplicable methods.
 */
public interface IRepository {

    // BASIC CRUD OPERATIONS

    /**
     * Insert a new entity into the database
     * @param entity - the entity to insert
     * @throws SQLException if database error occurs
     */
    void insert(Object entity) throws SQLException;

    /**
     * Find an entity by its unique ID
     * @param id - the entity's UUID
     * @return Optional containing entity if found, empty otherwise
     * @throws SQLException if database error occurs
     */
    Optional<?> findById(UUID id) throws SQLException;

    /**
     * Update an existing entity
     * @param entity - the entity with updated data
     * @throws SQLException if database error occurs
     */
    void update(Object entity) throws SQLException;

    /**
     * Delete an entity by its ID
     * @param id - the entity's UUID
     * @throws SQLException if database error occurs
     */
    void delete(UUID id) throws SQLException;

    /**
     * List all entities of this type
     * @return List of all entities
     * @throws SQLException if database error occurs
     */
    List<?> listAll() throws SQLException;

    // QUERY OPERATIONS

    /**
     * List entities with a search query or filter
     * @param query - search term or filter parameter
     * @return List of matching entities
     * @throws SQLException if database error occurs
     */
    List<?> listByQuery(String query) throws SQLException;

    /**
     * List entities by a related entity ID (e.g., ratings by media ID)
     * @param relatedId - the related entity's UUID
     * @return List of entities related to this ID
     * @throws SQLException if database error occurs
     */
    List<?> listByRelatedId(UUID relatedId) throws SQLException;

    // LOOKUP OPERATIONS

    /**
     * Find entity by a string identifier (e.g., username, token)
     * @param identifier - the string to search for
     * @return Optional containing entity if found, empty otherwise
     * @throws SQLException if database error occurs
     */
    Optional<?> findByString(String identifier) throws SQLException;
}
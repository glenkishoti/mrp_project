package at.fhtw.mrp.service;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.MediaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MediaService
 * Tests validation and business logic for media operations
 */
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    private MediaService mediaService;
    private User testOwner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mediaService = new MediaService(mediaRepository);
        testOwner = new User(UUID.randomUUID(), "testowner", "hashedpw", null);
    }

    @Test
    @DisplayName("Test 17: Create media with valid required fields succeeds")
    void testCreateMedia_ValidRequiredFields() throws SQLException {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        body.put("title", "The Matrix");
        body.put("description", "Sci-fi classic");
        body.put("mediaType", "movie");
        body.put("releaseYear", 1999);
        body.put("genres", "sci-fi,action");
        body.put("ageRestriction", 16);

        doNothing().when(mediaRepository).insert(any());

        // Act
        UUID mediaId = mediaService.createFromRequest(testOwner, body);

        // Assert
        assertNotNull(mediaId, "Create should return a media ID");
        verify(mediaRepository).insert(any());
    }

    @Test
    @DisplayName("Test 18: Create media with minimal fields succeeds")
    void testCreateMedia_MinimalFields() throws SQLException {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Simple Movie");
        body.put("description", "A description");
        body.put("mediaType", "movie");
        // Optional fields not provided

        doNothing().when(mediaRepository).insert(any());

        // Act
        UUID mediaId = mediaService.createFromRequest(testOwner, body);

        // Assert
        assertNotNull(mediaId, "Create should return a media ID even with minimal fields");
        verify(mediaRepository).insert(any());
    }

    @Test
    @DisplayName("Test 19: Create media with all nullable fields set to null succeeds")
    void testCreateMedia_NullableFields() throws SQLException {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Movie Title");
        body.put("description", "Description");
        body.put("mediaType", "series");
        body.put("releaseYear", null);
        body.put("genres", null);
        body.put("ageRestriction", null);

        doNothing().when(mediaRepository).insert(any());

        // Act
        UUID mediaId = mediaService.createFromRequest(testOwner, body);

        // Assert
        assertNotNull(mediaId, "Create should succeed with null optional fields");
        verify(mediaRepository).insert(any());
    }

    @Test
    @DisplayName("Test 20: Create media returns unique IDs")
    void testCreateMedia_UniqueIds() throws SQLException {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Movie");
        body.put("description", "Desc");
        body.put("mediaType", "movie");

        doNothing().when(mediaRepository).insert(any());

        // Act
        UUID mediaId1 = mediaService.createFromRequest(testOwner, body);
        UUID mediaId2 = mediaService.createFromRequest(testOwner, body);

        // Assert
        assertNotNull(mediaId1);
        assertNotNull(mediaId2);
        assertNotEquals(mediaId1, mediaId2, "Each creation should generate a unique ID");
        verify(mediaRepository, times(2)).insert(any());
    }
}
package at.fhtw.mrp.service;

import at.fhtw.mrp.model.Rating;
import at.fhtw.mrp.repo.RatingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RatingService
 * Tests validation logic and business rules
 * FIXED: Mock insert() to set ID on rating object
 */
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ratingService = new RatingService(ratingRepository);
    }

    @Test
    @DisplayName("Test 12: Create rating with valid stars (1-5) succeeds")
    void testCreateRating_ValidStars() throws SQLException {
        // Arrange
        UUID mediaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        int validStars = 4;
        String comment = "Great movie!";

        // FIXED: Mock insert to set the ID on the rating
        doAnswer(invocation -> {
            Rating rating = invocation.getArgument(0);
            rating.setId(UUID.randomUUID());
            return null;
        }).when(ratingRepository).insert(any());

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            UUID ratingId = ratingService.create(mediaId, userId, validStars, comment);
            assertNotNull(ratingId, "Create should return a rating ID");
        }, "Creating rating with valid stars (1-5) should succeed");

        verify(ratingRepository).insert(any());
    }

    @Test
    @DisplayName("Test 13: Create rating with stars < 1 throws exception")
    void testCreateRating_StarsLessThanOne() {
        // Arrange
        UUID mediaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        int invalidStars = 0;
        String comment = "Invalid rating";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ratingService.create(mediaId, userId, invalidStars, comment);
        }, "Stars less than 1 should throw IllegalArgumentException");

        // Verify exception message
        assertTrue(exception.getMessage().contains("1") || exception.getMessage().contains("5"),
                "Exception message should mention valid range");

        verifyNoInteractions(ratingRepository);
    }

    @Test
    @DisplayName("Test 14: Create rating with stars > 5 throws exception")
    void testCreateRating_StarsGreaterThanFive() {
        // Arrange
        UUID mediaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        int invalidStars = 6;
        String comment = "Invalid rating";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ratingService.create(mediaId, userId, invalidStars, comment);
        }, "Stars greater than 5 should throw IllegalArgumentException");

        // Verify exception message
        assertTrue(exception.getMessage().contains("1") || exception.getMessage().contains("5"),
                "Exception message should mention valid range");

        verifyNoInteractions(ratingRepository);
    }

    @Test
    @DisplayName("Test 15: Create rating with null comment succeeds")
    void testCreateRating_NullComment() throws SQLException {
        // Arrange
        UUID mediaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        int validStars = 5;

        // FIXED: Mock insert to set the ID on the rating
        doAnswer(invocation -> {
            Rating rating = invocation.getArgument(0);
            rating.setId(UUID.randomUUID());
            return null;
        }).when(ratingRepository).insert(any());

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            UUID ratingId = ratingService.create(mediaId, userId, validStars, null);
            assertNotNull(ratingId, "Create should return a rating ID even with null comment");
        }, "Creating rating with null comment should succeed (comment is optional)");

        verify(ratingRepository).insert(any());
    }

    @Test
    @DisplayName("Test 16: Create rating with empty comment succeeds")
    void testCreateRating_EmptyComment() throws SQLException {
        // Arrange
        UUID mediaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        int validStars = 3;
        String emptyComment = "";

        // FIXED: Mock insert to set the ID on the rating
        doAnswer(invocation -> {
            Rating rating = invocation.getArgument(0);
            rating.setId(UUID.randomUUID());
            return null;
        }).when(ratingRepository).insert(any());

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            UUID ratingId = ratingService.create(mediaId, userId, validStars, emptyComment);
            assertNotNull(ratingId, "Create should return a rating ID even with empty comment");
        }, "Creating rating with empty comment should succeed");

        verify(ratingRepository).insert(any());
    }
}
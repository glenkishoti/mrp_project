package at.fhtw.mrp.service;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.repo.FavoriteRepository;
import at.fhtw.mrp.repo.MediaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FavoriteService
 * Tests favorite functionality
 */
class FavouriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private MediaRepository mediaRepository;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        favoriteService = new FavoriteService(favoriteRepository, mediaRepository);
    }

    @Test
    @DisplayName("Test 9: Add favorite with valid media succeeds")
    void testAddFavorite_ValidMedia() throws SQLException {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        MediaEntry media = new MediaEntry(
                mediaId,
                UUID.randomUUID(),
                "The Matrix",
                "Sci-fi",
                "movie",
                1999,
                "action",
                16
        );

        // Mock that media exists - Use doReturn to avoid type checking issues
        doReturn(Optional.of(media)).when(mediaRepository).findById(mediaId);
        doNothing().when(favoriteRepository).addFavorite(userId, mediaId);

        // Act
        assertDoesNotThrow(() -> {
            favoriteService.addFavorite(userId, mediaId);
        }, "Adding favorite with valid media should succeed");

        // Assert
        verify(mediaRepository).findById(mediaId);
        verify(favoriteRepository).addFavorite(userId, mediaId);
    }

    @Test
    @DisplayName("Test 10: Add favorite with non-existent media throws exception")
    void testAddFavorite_NonExistentMedia() throws SQLException {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID nonExistentMediaId = UUID.randomUUID();

        // Mock that media does NOT exist - Use doReturn to avoid type checking issues
        doReturn(Optional.empty()).when(mediaRepository).findById(nonExistentMediaId);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            favoriteService.addFavorite(userId, nonExistentMediaId);
        }, "Adding favorite with non-existent media should throw exception");

        verify(mediaRepository).findById(nonExistentMediaId);
        verify(favoriteRepository, never()).addFavorite(any(), any());
    }

    @Test
    @DisplayName("Test 11: Get user favorites returns correct list")
    void testGetUserFavorites_ReturnsCorrectList() throws SQLException {
        // Arrange
        UUID userId = UUID.randomUUID();

        MediaEntry media1 = new MediaEntry(
                UUID.randomUUID(), UUID.randomUUID(), "Movie 1", "Desc", "movie", 2020, "action", 12
        );
        MediaEntry media2 = new MediaEntry(
                UUID.randomUUID(), UUID.randomUUID(), "Movie 2", "Desc", "series", 2021, "drama", 16
        );

        List<MediaEntry> expectedFavorites = Arrays.asList(media1, media2);

        // Mock repository returning favorite media directly
        when(favoriteRepository.getFavoriteMedia(userId)).thenReturn(expectedFavorites);

        // Act
        List<MediaEntry> favorites = favoriteService.getUserFavorites(userId);

        // Assert
        assertNotNull(favorites, "Favorites list should not be null");
        assertEquals(2, favorites.size(), "Should return 2 favorite media entries");
        assertTrue(favorites.contains(media1), "Should contain first media");
        assertTrue(favorites.contains(media2), "Should contain second media");

        verify(favoriteRepository).getFavoriteMedia(userId);
    }
}
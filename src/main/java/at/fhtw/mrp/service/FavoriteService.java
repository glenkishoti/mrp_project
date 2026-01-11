package at.fhtw.mrp.service;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.repo.FavoriteRepository;
import at.fhtw.mrp.repo.MediaRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

//Service implementation for Favorite business logic

public class FavoriteService implements IService {

    private final FavoriteRepository favoriteRepo;
    private final MediaRepository mediaRepo;

    public FavoriteService(FavoriteRepository favoriteRepo, MediaRepository mediaRepo) {
        this.favoriteRepo = favoriteRepo;
        this.mediaRepo = mediaRepo;
    }

    @Override
    public UUID create(UUID userId, Map<String, Object> data) throws SQLException {
        String mediaIdStr = (String) data.get("mediaId");
        UUID mediaId = UUID.fromString(mediaIdStr);

        // Validate that media exists
        if (mediaRepo.findById(mediaId).isEmpty()) {
            throw new IllegalArgumentException("Media entry not found");
        }

        favoriteRepo.addFavorite(userId, mediaId);
        return UUID.randomUUID();
    }

    @Override
    public Optional<?> get(UUID id) throws SQLException {
        return favoriteRepo.findById(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MediaEntry> list(String query) throws SQLException {
        // For favorites, we ignore the query and return all
        return (List<MediaEntry>) favoriteRepo.listAll();
    }

    @Override
    public List<MediaEntry> listByUser(UUID userId) throws SQLException {
        return favoriteRepo.getFavoriteMedia(userId);
    }

    @Override
    public void update(UUID id, UUID userId, Map<String, Object> data) throws SQLException {
        throw new UnsupportedOperationException("Favorites cannot be updated, delete and recreate instead");
    }

    @Override
    public void delete(UUID id, UUID userId) throws SQLException {
        favoriteRepo.delete(id);
    }

    // HELPER METHODS (not from IService interface)

    public void addFavorite(UUID userId, UUID mediaId) throws SQLException {
        // Validate that media exists
        if (mediaRepo.findById(mediaId).isEmpty()) {
            throw new IllegalArgumentException("Media entry not found");
        }

        favoriteRepo.addFavorite(userId, mediaId);
    }

    public void removeFavorite(UUID userId, UUID mediaId) throws SQLException {
        favoriteRepo.removeFavorite(userId, mediaId);
    }

    public boolean toggleFavorite(UUID userId, UUID mediaId) throws SQLException {
        // Check if already favorited
        if (favoriteRepo.isFavorite(userId, mediaId)) {
            // Already favorited, then remove it
            favoriteRepo.removeFavorite(userId, mediaId);
            return false;  // Removed
        } else {
            // Not favorited, then add it
            addFavorite(userId, mediaId);
            return true;   // Added
        }
    }

    public boolean isFavorite(UUID userId, UUID mediaId) throws SQLException {
        return favoriteRepo.isFavorite(userId, mediaId);
    }

    public List<MediaEntry> getUserFavorites(UUID userId) throws SQLException {
        return favoriteRepo.getFavoriteMedia(userId);
    }

    public int getFavoriteCount(UUID mediaId) throws SQLException {
        return favoriteRepo.getFavoriteCount(mediaId);
    }

    // UNUSED METHODS FROM ISERVICE (not needed in FavouriteService)

    @Override
    public String authenticate(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for FavoriteService");
    }

    @Override
    public UUID register(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for FavoriteService");
    }
}
package at.fhtw.mrp.service;

import at.fhtw.mrp.model.Rating;
import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.repo.RatingRepository;
import at.fhtw.mrp.repo.MediaRepository;
import at.fhtw.mrp.repo.FavoriteRepository;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for user profile statistics
 * Provides: total ratings, average score given, favorite genre, media created count
 */
public class UserProfileService {

    private final RatingRepository ratingRepo;
    private final MediaRepository mediaRepo;
    private final FavoriteRepository favoriteRepo;

    public UserProfileService(RatingRepository ratingRepo, MediaRepository mediaRepo, FavoriteRepository favoriteRepo) {
        this.ratingRepo = ratingRepo;
        this.mediaRepo = mediaRepo;
        this.favoriteRepo = favoriteRepo;
    }

    /**
     * Get complete user statistics
     * @param userId User's UUID
     * @return Map with statistics
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserStatistics(UUID userId) throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        // Get user's ratings
        List<Rating> userRatings = ratingRepo.listByUser(userId);

        // Total ratings given
        stats.put("totalRatingsGiven", userRatings.size());

        // Average score given by user
        if (!userRatings.isEmpty()) {
            double avgScore = userRatings.stream()
                    .mapToInt(Rating::getStars)
                    .average()
                    .orElse(0.0);
            stats.put("averageScoreGiven", Math.round(avgScore * 100.0) / 100.0);
        } else {
            stats.put("averageScoreGiven", 0.0);
        }

        // Get user's created media
        List<MediaEntry> userMedia = (List<MediaEntry>) mediaRepo.listByRelatedId(userId);
        stats.put("totalMediaCreated", userMedia.size());

        // Get user's favorites
        List<MediaEntry> userFavorites = favoriteRepo.getFavoriteMedia(userId);
        stats.put("totalFavorites", userFavorites.size());

        // Favorite genre (most common genre in user's favorites)
        Map<String, Integer> genreCounts = new HashMap<>();
        for (MediaEntry media : userFavorites) {
            if (media.getGenres() != null && !media.getGenres().isBlank()) {
                String[] genres = media.getGenres().split(",");
                for (String genre : genres) {
                    String trimmed = genre.trim().toLowerCase();
                    genreCounts.put(trimmed, genreCounts.getOrDefault(trimmed, 0) + 1);
                }
            }
        }

        String favoriteGenre = genreCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("none");

        stats.put("favoriteGenre", favoriteGenre);

        // Top genres (up to 3)
        List<String> topGenres = genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        stats.put("topGenres", topGenres);

        // Average rating received on user's media
        double totalReceivedRating = 0.0;
        int ratingCount = 0;

        for (MediaEntry media : userMedia) {
            double avgScore = mediaRepo.averageScore(media.getId());
            if (avgScore > 0) {
                totalReceivedRating += avgScore;
                ratingCount++;
            }
        }

        if (ratingCount > 0) {
            stats.put("averageRatingReceived", Math.round((totalReceivedRating / ratingCount) * 100.0) / 100.0);
        } else {
            stats.put("averageRatingReceived", 0.0);
        }

        return stats;
    }

    /**
     * Get user's activity summary
     * @param userId User's UUID
     * @return Map with activity data
     */
    public Map<String, Object> getUserActivity(UUID userId) throws SQLException {
        Map<String, Object> activity = new HashMap<>();

        List<Rating> userRatings = ratingRepo.listByUser(userId);

        // Most recent rating
        if (!userRatings.isEmpty()) {
            Rating recent = userRatings.get(0); // Already sorted by created_at DESC
            Map<String, Object> recentRating = new HashMap<>();
            recentRating.put("id", recent.getId());
            recentRating.put("mediaId", recent.getMediaId());
            recentRating.put("stars", recent.getStars());
            recentRating.put("comment", recent.getComment());
            activity.put("mostRecentRating", recentRating);
        } else {
            activity.put("mostRecentRating", null);
        }

        // Ratings distribution (how many 1-star, 2-star, etc.)
        Map<Integer, Long> distribution = userRatings.stream()
                .collect(Collectors.groupingBy(Rating::getStars, Collectors.counting()));

        activity.put("ratingsDistribution", distribution);

        return activity;
    }
}
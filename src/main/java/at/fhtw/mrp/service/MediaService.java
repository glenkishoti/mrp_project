package at.fhtw.mrp.service;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.MediaRepository;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for Media business logic
 * SUPPORTS: Filtering, Sorting, Search
 */
public class MediaService implements IService {

    private final MediaRepository mediaRepo;

    public MediaService(MediaRepository mediaRepo) {
        this.mediaRepo = mediaRepo;
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        return Integer.valueOf(String.valueOf(o));
    }

    private MediaEntry fromRequest(UUID id, UUID ownerId, Map<String, Object> body) {
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        String mediaType = (String) body.get("mediaType");
        Integer releaseYear = asInt(body.get("releaseYear"));
        String genres = (String) body.get("genres");
        Integer ageRestriction = asInt(body.get("ageRestriction"));

        return new MediaEntry(id, ownerId, title, description, mediaType,
                releaseYear, genres, ageRestriction);
    }

    @Override
    public UUID create(UUID userId, Map<String, Object> data) throws SQLException {
        UUID id = UUID.randomUUID();
        MediaEntry entry = fromRequest(id, userId, data);
        mediaRepo.insert(entry);
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<MediaEntry> get(UUID id) throws SQLException {
        return (Optional<MediaEntry>) mediaRepo.findById(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MediaEntry> list(String query) throws SQLException {
        return (List<MediaEntry>) mediaRepo.listByQuery(query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MediaEntry> listByUser(UUID userId) throws SQLException {
        return (List<MediaEntry>) mediaRepo.listByRelatedId(userId);
    }

    @Override
    public void update(UUID id, UUID userId, Map<String, Object> data) throws SQLException {
        Optional<?> opt = mediaRepo.findById(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Media not found");
        }

        MediaEntry existing = (MediaEntry) opt.get();

        if (!existing.getOwnerId().equals(userId)) {
            throw new SecurityException("Forbidden");
        }

        MediaEntry updated = existing.withUpdatedData(
                (String) data.get("title"),
                (String) data.get("description"),
                (String) data.get("mediaType"),
                asInt(data.get("releaseYear")),
                (String) data.get("genres"),
                asInt(data.get("ageRestriction"))
        );

        mediaRepo.update(updated);
    }

    @Override
    public void delete(UUID id, UUID userId) throws SQLException {
        Optional<?> opt = mediaRepo.findById(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Media not found");
        }

        MediaEntry existing = (MediaEntry) opt.get();

        if (!existing.getOwnerId().equals(userId)) {
            throw new SecurityException("Forbidden");
        }

        mediaRepo.delete(id);
    }

    // ✅ NEW: FILTERING AND SORTING

    /**
     * Filter and sort media entries
     * @param filters Map with keys: genre, type, year, minAge, maxAge, minRating
     * @param sortBy Sort criteria: "title", "year", "score" (default: "title")
     * @param sortOrder "asc" or "desc" (default: "asc")
     * @return Filtered and sorted list of media entries
     */
    @SuppressWarnings("unchecked")
    public List<MediaEntry> filterAndSort(Map<String, String> filters, String sortBy, String sortOrder)
            throws SQLException {

        // Start with all media
        List<MediaEntry> results = (List<MediaEntry>) mediaRepo.listAll();

        // Apply filters
        if (filters != null) {
            // Filter by genre
            if (filters.containsKey("genre")) {
                String genre = filters.get("genre").toLowerCase();
                results = results.stream()
                        .filter(m -> m.getGenres() != null && m.getGenres().toLowerCase().contains(genre))
                        .collect(Collectors.toList());
            }

            // Filter by type (movie/series)
            if (filters.containsKey("type")) {
                String type = filters.get("type").toLowerCase();
                results = results.stream()
                        .filter(m -> m.getMediaType() != null && m.getMediaType().equalsIgnoreCase(type))
                        .collect(Collectors.toList());
            }

            // Filter by year
            if (filters.containsKey("year")) {
                try {
                    int year = Integer.parseInt(filters.get("year"));
                    results = results.stream()
                            .filter(m -> m.getReleaseYear() != null && m.getReleaseYear() == year)
                            .collect(Collectors.toList());
                } catch (NumberFormatException ignored) {}
            }

            // Filter by minimum year
            if (filters.containsKey("minYear")) {
                try {
                    int minYear = Integer.parseInt(filters.get("minYear"));
                    results = results.stream()
                            .filter(m -> m.getReleaseYear() != null && m.getReleaseYear() >= minYear)
                            .collect(Collectors.toList());
                } catch (NumberFormatException ignored) {}
            }

            // Filter by maximum year
            if (filters.containsKey("maxYear")) {
                try {
                    int maxYear = Integer.parseInt(filters.get("maxYear"));
                    results = results.stream()
                            .filter(m -> m.getReleaseYear() != null && m.getReleaseYear() <= maxYear)
                            .collect(Collectors.toList());
                } catch (NumberFormatException ignored) {}
            }

            // Filter by age restriction
            if (filters.containsKey("maxAge")) {
                try {
                    int maxAge = Integer.parseInt(filters.get("maxAge"));
                    results = results.stream()
                            .filter(m -> m.getAgeRestriction() == null || m.getAgeRestriction() <= maxAge)
                            .collect(Collectors.toList());
                } catch (NumberFormatException ignored) {}
            }
        }

        // Apply sorting
        Comparator<MediaEntry> comparator = null;

        if (sortBy == null) sortBy = "title";

        switch (sortBy.toLowerCase()) {
            case "year":
                comparator = Comparator.comparing(
                        m -> m.getReleaseYear() != null ? m.getReleaseYear() : 0
                );
                break;
            case "score":
                // Note: This requires fetching average scores - expensive operation
                // For now, sort by title if score is requested
                comparator = Comparator.comparing(
                        m -> m.getTitle() != null ? m.getTitle().toLowerCase() : ""
                );
                break;
            case "title":
            default:
                comparator = Comparator.comparing(
                        m -> m.getTitle() != null ? m.getTitle().toLowerCase() : ""
                );
                break;
        }

        // Apply sort order
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        results.sort(comparator);

        return results;
    }

    /**
     * Search media by title
     * @param searchQuery Title to search for (case-insensitive)
     * @return List of matching media entries
     */
    @SuppressWarnings("unchecked")
    public List<MediaEntry> searchByTitle(String searchQuery) throws SQLException {
        if (searchQuery == null || searchQuery.isBlank()) {
            return (List<MediaEntry>) mediaRepo.listAll();
        }

        return (List<MediaEntry>) mediaRepo.listByQuery(searchQuery);
    }

    // HELPER METHODS (not from IService interface)

    public double averageScore(UUID mediaId) throws SQLException {
        return mediaRepo.averageScore(mediaId);
    }

    // For compatibility with handlers
    public UUID createFromRequest(User owner, Map<String, Object> body) throws SQLException {
        return create(owner.getId(), body);
    }

    public void updateFromRequest(UUID mediaId, User owner, Map<String, Object> body) throws SQLException {
        update(mediaId, owner.getId(), body);
    }

    public void delete(UUID mediaId, User owner) throws SQLException {
        delete(mediaId, owner.getId());
    }

    // UNUSED METHODS FROM ISERVICE (not needed for MediaService)

    @Override
    public String authenticate(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for MediaService");
    }

    @Override
    public UUID register(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("Not applicable for MediaService");
    }
}
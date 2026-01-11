package at.fhtw.mrp.service;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.MediaRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


// Service implementation for Media business logic

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
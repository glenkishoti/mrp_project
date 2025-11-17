package at.fhtw.mrp.service;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.IMediaRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MediaService implements IMediaService {

    private final IMediaRepository mediaRepo;

    public MediaService(IMediaRepository mediaRepo) {
        this.mediaRepo = mediaRepo;
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        return Integer.valueOf(String.valueOf(o));
    }

    private MediaEntry fromRequest(UUID id, User owner, Map<String, Object> body) {
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        String mediaType = (String) body.get("mediaType");
        Integer releaseYear = asInt(body.get("releaseYear"));
        String genres = (String) body.get("genres");
        Integer ageRestriction = asInt(body.get("ageRestriction"));

        return new MediaEntry(id, owner.getId(), title, description, mediaType,
                releaseYear, genres, ageRestriction);
    }

    @Override
    public UUID createFromRequest(User owner, Map<String, Object> body) throws SQLException {
        UUID id = UUID.randomUUID();
        MediaEntry entry = fromRequest(id, owner, body);
        mediaRepo.insert(entry);
        return id;
    }

    @Override
    public Optional<MediaEntry> get(UUID id) throws SQLException {
        return mediaRepo.findById(id);
    }

    @Override
    public List<MediaEntry> list(String query) throws SQLException {
        return mediaRepo.list(query);
    }

    @Override
    public double averageScore(UUID mediaId) throws SQLException {
        return mediaRepo.averageScore(mediaId);
    }

    @Override
    public void updateFromRequest(UUID mediaId, User owner, Map<String, Object> body) throws SQLException {
        MediaEntry existing = mediaRepo.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));

        if (!existing.getOwnerId().equals(owner.getId())) {
            throw new SecurityException("Forbidden");
        }

        MediaEntry updated = existing.withUpdatedData(
                (String) body.get("title"),
                (String) body.get("description"),
                (String) body.get("mediaType"),
                asInt(body.get("releaseYear")),
                (String) body.get("genres"),
                asInt(body.get("ageRestriction"))
        );

        mediaRepo.update(updated);
    }

    @Override
    public void delete(UUID mediaId, User owner) throws SQLException {
        MediaEntry existing = mediaRepo.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found"));

        if (!existing.getOwnerId().equals(owner.getId())) {
            throw new SecurityException("Forbidden");
        }

        mediaRepo.delete(mediaId);
    }
}

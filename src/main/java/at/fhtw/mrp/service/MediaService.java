package at.fhtw.mrp.service;

import at.fhtw.mrp.model.MediaEntry;
import at.fhtw.mrp.repo.MediaRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class MediaService {
    private final MediaRepository repo;

    public MediaService(MediaRepository repo) { this.repo = repo; }

    public int create(int ownerId, String title, String description, String type,
                      Integer year, String genres, Integer age) throws SQLException {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title required");
        if (type == null || !(type.equals("movie") || type.equals("series") || type.equals("game")))
            throw new IllegalArgumentException("mediaType must be movie|series|game");
        return repo.insert(ownerId, title.trim(), description, type, year, genres, age);
    }

    public Optional<MediaEntry> get(int id) throws SQLException { return repo.findById(id); }

    public List<MediaEntry> list(String query) throws SQLException { return repo.findAll(query); }

    public void update(int id, int ownerId, String title, String description, String type,
                       Integer year, String genres, Integer age) throws SQLException {
        int changed = repo.update(id, ownerId, title, description, type, year, genres, age);
        if (changed == 0) throw new SecurityException("Not owner or not found");
    }

    public void delete(int id, int ownerId) throws SQLException {
        int changed = repo.delete(id, ownerId);
        if (changed == 0) throw new SecurityException("Not owner or not found");
    }

    public Double averageScore(int mediaId) throws SQLException { return repo.averageScore(mediaId); }
}

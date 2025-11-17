package at.fhtw.mrp.model;

import java.util.UUID;

public class MediaEntry {
    private final UUID id;
    private final UUID ownerId;
    private final String title;
    private final String description;
    private final String mediaType;
    private final Integer releaseYear;
    private final String genres;
    private final Integer ageRestriction;

    public MediaEntry(UUID id,
                      UUID ownerId,
                      String title,
                      String description,
                      String mediaType,
                      Integer releaseYear,
                      String genres,
                      Integer ageRestriction) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.mediaType = mediaType;
        this.releaseYear = releaseYear;
        this.genres = genres;
        this.ageRestriction = ageRestriction;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getMediaType() {
        return mediaType;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public String getGenres() {
        return genres;
    }

    public Integer getAgeRestriction() {
        return ageRestriction;
    }

    public MediaEntry withId(UUID newId) {
        return new MediaEntry(newId, ownerId, title, description, mediaType,
                releaseYear, genres, ageRestriction);
    }

    public MediaEntry withUpdatedData(String title,
                                      String description,
                                      String mediaType,
                                      Integer releaseYear,
                                      String genres,
                                      Integer ageRestriction) {
        return new MediaEntry(id, ownerId, title, description, mediaType,
                releaseYear, genres, ageRestriction);
    }
}

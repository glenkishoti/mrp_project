package at.fhtw.mrp.model;

public class MediaEntry {
    private final int id;
    private final int ownerId;
    private final String title;
    private final String description;
    private final String mediaType;     // "movie" | "series" | "game"
    private final Integer releaseYear;
    private final String genres;        // comma-separated
    private final Integer ageRestriction;

    public MediaEntry(int id, int ownerId, String title, String description,
                      String mediaType, Integer releaseYear, String genres, Integer ageRestriction) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.mediaType = mediaType;
        this.releaseYear = releaseYear;
        this.genres = genres;
        this.ageRestriction = ageRestriction;
    }

    public int getId() { return id; }
    public int getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getMediaType() { return mediaType; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getGenres() { return genres; }
    public Integer getAgeRestriction() { return ageRestriction; }
}

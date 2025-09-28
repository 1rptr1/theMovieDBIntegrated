package com.integrated.imdb.dto;

import java.util.Objects;

/**
 * Data Transfer Object for Movie information.
 * Contains both basic movie details and enriched data from OMDb.
 */
public class MovieDto {
    // Basic movie information
    private String tconst;           // IMDb unique identifier
    private String primaryTitle;     // Primary title of the movie
    private String startYear;        // Year the movie was released
    private String genres;           // Comma-separated list of genres
    private Double averageRating;    // Average rating from IMDb
    private Integer numVotes;        // Number of votes for the rating
    
    // Additional movie details (from OMDb)
    private String plot;             // Movie plot/summary
    private String poster;           // URL to the movie poster
    private String runtime;          // Runtime in minutes (as string for display)
    private String director;         // Movie director(s)
    private String cast;             // Main cast members
    
    // Additional fields for internal use
    private String actorName;        // Primary actor name (for search results)
    
    /**
     * Sets the runtime from an integer value (in minutes).
     * Converts to a formatted string (e.g., "120 min").
     * 
     * @param minutes Runtime in minutes
     */
    public void setRuntimeFromMinutes(int minutes) {
        this.runtime = minutes > 0 ? minutes + " min" : "N/A";
    }
    
    /**
     * Gets the runtime as an integer (in minutes).
     * Extracts the numeric part from the runtime string.
     * 
     * @return Runtime in minutes, or 0 if not available
     */
    public int getRuntimeInMinutes() {
        if (runtime == null || runtime.isEmpty()) {
            return 0;
        }
        try {
            // Extract first sequence of digits
            String numStr = runtime.replaceAll("^\\D*(\\d+).*$", "$1");
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Gets the comma-separated list of genres.
     * 
     * @return The genres string
     */
    public String getGenres() {
        return genres;
    }
    
    /**
     * Sets the comma-separated list of genres.
     * 
     * @param genres The genres to set
     */
    public void setGenres(String genres) {
        this.genres = genres;
    }

    // Getters and setters for all fields
    public String getTconst() {
        return tconst;
    }

    public void setTconst(String tconst) {
        this.tconst = tconst;
    }

    public String getPrimaryTitle() {
        return primaryTitle;
    }

    public void setPrimaryTitle(String primaryTitle) {
        this.primaryTitle = primaryTitle;
    }

    public String getStartYear() {
        return startYear;
    }

    public void setStartYear(String startYear) {
        this.startYear = startYear;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getNumVotes() {
        return numVotes;
    }

    public void setNumVotes(Integer numVotes) {
        this.numVotes = numVotes;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovieDto movieDto = (MovieDto) o;
        return Objects.equals(tconst, movieDto.tconst) &&
               Objects.equals(primaryTitle, movieDto.primaryTitle) &&
               Objects.equals(startYear, movieDto.startYear) &&
               Objects.equals(genres, movieDto.genres) &&
               Objects.equals(averageRating, movieDto.averageRating) &&
               Objects.equals(numVotes, movieDto.numVotes) &&
               Objects.equals(plot, movieDto.plot) &&
               Objects.equals(poster, movieDto.poster) &&
               Objects.equals(runtime, movieDto.runtime) &&
               Objects.equals(director, movieDto.director) &&
               Objects.equals(cast, movieDto.cast) &&
               Objects.equals(actorName, movieDto.actorName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tconst, primaryTitle, startYear, genres, averageRating, numVotes, plot, poster, runtime, director, cast, actorName);
    }

    @Override
    public String toString() {
        return "MovieDto{" +
               "tconst='" + tconst + '\'' +
               ", primaryTitle='" + primaryTitle + '\'' +
               ", startYear='" + startYear + '\'' +
               ", genres='" + genres + '\'' +
               ", averageRating=" + averageRating +
               ", numVotes=" + numVotes +
               ", plot='" + plot + '\'' +
               ", poster='" + poster + '\'' +
               ", runtime='" + runtime + '\'' +
               ", director='" + director + '\'' +
               ", cast='" + cast + '\'' +
               ", actorName='" + actorName + '\'' +
               '}';
    }

    // Constructors
    public MovieDto() {
    }

    public MovieDto(String tconst, String primaryTitle, String startYear, String genres, Double averageRating, Integer numVotes, String plot, String poster, String runtime, String director, String cast, String actorName) {
        this.tconst = tconst;
        this.primaryTitle = primaryTitle;
        this.startYear = startYear;
        this.genres = genres;
        this.averageRating = averageRating;
        this.numVotes = numVotes;
        this.plot = plot;
        this.poster = poster;
        this.runtime = runtime;
        this.director = director;
        this.cast = cast;
        this.actorName = actorName;
    }
}

package com.integrated.imdb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Movie information.
 * Contains both basic movie details and enriched data from OMDb.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}

package com.integrated.imdb.service;

import com.integrated.imdb.dto.MovieDto;
import com.integrated.imdb.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for handling movie-related business logic.
 * Provides methods for searching, filtering, and retrieving movie details.
 */

@Service
public class MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final MovieRepository movieRepository;
    private final OmdbClient omdbClient;
    
    private static final int DEFAULT_MOVIE_LIMIT = 20;
    /**
     * Minimum number of votes a movie must have to be considered for recommendations.
     * This helps filter out less popular or less rated movies.
     */
    private static final int MIN_VOTES_THRESHOLD = 1000; // Minimum votes to consider a movie for recommendations

    /**
     * Constructs a new MovieService with the required dependencies.
     * 
     * @param movieRepository The repository for movie data access
     * @param omdbClient The client for OMDb API integration
     */
    @Autowired
    public MovieService(MovieRepository movieRepository, OmdbClient omdbClient) {
        this.movieRepository = movieRepository;
        this.omdbClient = omdbClient;
        log.info("MovieService initialized with repository: {}", movieRepository != null ? "present" : "null");
    }

    /**
     * Get top movies by actor with plot details
     */
    public List<MovieDto> getTopMoviesByActor(String actor, int limit) {
        log.info("Fetching top {} movies for actor: {}", limit, actor);
        List<Map<String, Object>> movies = movieRepository.findTopMoviesByActor(actor, limit);
        return enrichMoviesWithOmdb(movies);
    }

    /**
     * Search movies by title with plot details
     */
    public List<MovieDto> searchMoviesByTitle(String title, int limit) {
        log.info("Searching movies with title: {}", title);
        List<Map<String, Object>> movies = movieRepository.searchMoviesByTitle(title, limit);
        return enrichMoviesWithOmdb(movies);
    }

    /**
     * Get top rated movies with plot details
     */
    /**
     * Get top rated movies with plot details
     * 
     * @param limit Maximum number of movies to return
     * @return List of top rated movies as DTOs
     */
    public List<MovieDto> getTopRatedMovies(int limit) {
        log.info("Fetching top {} rated movies with minimum {} votes", limit, MIN_VOTES_THRESHOLD);
        // Ensure we don't exceed our default limit
        int actualLimit = Math.min(limit, DEFAULT_MOVIE_LIMIT);
        List<Map<String, Object>> movies = movieRepository.getTopRatedMovies(actualLimit, MIN_VOTES_THRESHOLD);
        return enrichMoviesWithOmdb(movies);
    }

    /**
     * Get movie details by ID with full OMDb enrichment
     */
    public MovieDto getMovieById(String tconst) {
        log.info("Fetching movie details for: {}", tconst);
        Map<String, Object> movie = movieRepository.findMovieById(tconst);
        if (movie == null) {
            return null;
        }

        MovieDto dto = mapToMovieDto(movie);
        enrichSingleMovieWithOmdb(dto);
        
        // Get cast and crew
        List<Map<String, Object>> castCrew = movieRepository.getMovieCastAndCrew(tconst);
        String cast = castCrew.stream()
                .filter(cc -> "actor".equalsIgnoreCase((String) cc.get("category")) || 
                             "actress".equalsIgnoreCase((String) cc.get("category")))
                .map(cc -> (String) cc.get("primaryName"))
                .limit(5)
                .collect(Collectors.joining(", "));
        
        String director = castCrew.stream()
                .filter(cc -> "director".equalsIgnoreCase((String) cc.get("category")))
                .map(cc -> (String) cc.get("primaryName"))
                .findFirst()
                .orElse("");

        dto.setCast(cast);
        dto.setDirector(director);
        
        return dto;
    }

    /**
     * Filter movies with multiple criteria
     */
    public List<MovieDto> filterMovies(String actor, String genre, String fromYear, String toYear, int limit) {
        log.info("Filtering movies with criteria - actor: {}, genre: {}, years: {}-{}", 
                actor, genre, fromYear, toYear);
        List<Map<String, Object>> movies = movieRepository.filterMovies(actor, genre, fromYear, toYear, limit);
        return enrichMoviesWithOmdb(movies);
    }

    /**
     * Enrich list of movies with OMDb data
     */
    private List<MovieDto> enrichMoviesWithOmdb(List<Map<String, Object>> movies) {
        return movies.stream()
                .map(this::mapToMovieDto)
                .peek(this::enrichSingleMovieWithOmdb)
                .collect(Collectors.toList());
    }

    /**
     * Enrich single movie with OMDb data
     */
    private void enrichSingleMovieWithOmdb(MovieDto movie) {
        try {
            String tconst = movie.getTconst();
            if (tconst != null) {
                Map<String, Object> omdbData = omdbClient.fetchMovieDetails(tconst);
                if (omdbData != null) {
                    movie.setPlot((String) omdbData.getOrDefault("Plot", "Plot not available"));
                    movie.setPoster((String) omdbData.getOrDefault("Poster", ""));
                    movie.setRuntime((String) omdbData.getOrDefault("Runtime", ""));
                    
                    // If director not already set, get from OMDb
                    if (movie.getDirector() == null || movie.getDirector().isEmpty()) {
                        movie.setDirector((String) omdbData.getOrDefault("Director", ""));
                    }
                    
                    // If cast not already set, get from OMDb
                    if (movie.getCast() == null || movie.getCast().isEmpty()) {
                        movie.setCast((String) omdbData.getOrDefault("Actors", ""));
                    }
                } else {
                    setDefaultOmdbValues(movie);
                }
            } else {
                setDefaultOmdbValues(movie);
            }
        } catch (Exception e) {
            log.warn("Failed to enrich movie {} with OMDb data: {}", movie.getTconst(), e.getMessage());
            setDefaultOmdbValues(movie);
        }
    }

    private void setDefaultOmdbValues(MovieDto movie) {
        movie.setPlot("Plot not available");
        movie.setPoster("");
        movie.setRuntime("");
        if (movie.getDirector() == null) movie.setDirector("");
        if (movie.getCast() == null) movie.setCast("");
    }

    /**
     * Map database row to MovieDto
     */
    /**
     * Maps a database row to a MovieDto object.
     * 
     * @param row The database row as a map of column names to values
     * @return A populated MovieDto object
     */
    private MovieDto mapToMovieDto(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        
        MovieDto dto = new MovieDto();
        
        // Basic movie information
        dto.setTconst(getStringValue(row, "tconst"));
        dto.setPrimaryTitle(getStringValue(row, "primaryTitle"));
        dto.setStartYear(getStringValue(row, "startYear"));
        dto.setGenres(getStringValue(row, "genres"));
        dto.setActorName(getStringValue(row, "actorName"));
        
        // Ratings and votes
        dto.setAverageRating(getDoubleValue(row, "averageRating"));
        dto.setNumVotes(getIntegerValue(row, "numVotes"));
        
        // Handle runtime - can be either a string or an integer in the database
        Object runtime = row.get("runtimeMinutes");
        if (runtime != null) {
            if (runtime instanceof Number) {
                dto.setRuntimeFromMinutes(((Number) runtime).intValue());
            } else if (runtime instanceof String) {
                try {
                    int minutes = Integer.parseInt(runtime.toString());
                    dto.setRuntimeFromMinutes(minutes);
                } catch (NumberFormatException e) {
                    dto.setRuntime(runtime.toString());
                }
            }
        }
        
        return dto;
    }
    
    /**
     * Helper method to safely get a string value from a map.
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
    
    /**
     * Helper method to safely get an integer value from a map.
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Helper method to safely get a double value from a map.
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

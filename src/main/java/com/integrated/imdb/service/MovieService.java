package com.integrated.imdb.service;

import com.integrated.imdb.dto.MovieDto;
import com.integrated.imdb.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final MovieRepository movieRepository;
    private final OmdbClient omdbClient;

    public MovieService(MovieRepository movieRepository, OmdbClient omdbClient) {
        this.movieRepository = movieRepository;
        this.omdbClient = omdbClient;
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
    public List<MovieDto> getTopRatedMovies(int limit) {
        log.info("Fetching top {} rated movies", limit);
        List<Map<String, Object>> movies = movieRepository.getTopRatedMovies(limit);
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
    private MovieDto mapToMovieDto(Map<String, Object> row) {
        MovieDto dto = new MovieDto();
        dto.setTconst((String) row.get("tconst"));
        dto.setPrimaryTitle((String) row.get("primaryTitle"));
        dto.setStartYear((String) row.get("startYear"));
        dto.setGenres((String) row.get("genres"));
        dto.setActorName((String) row.get("actorName"));
        
        Object rating = row.get("averageRating");
        dto.setAverageRating(rating != null ? ((Number) rating).doubleValue() : null);
        
        Object votes = row.get("numVotes");
        dto.setNumVotes(votes != null ? ((Number) votes).intValue() : null);
        
        return dto;
    }
}

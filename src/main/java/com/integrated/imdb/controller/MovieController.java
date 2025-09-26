package com.integrated.imdb.controller;

import com.integrated.imdb.dto.*;
import com.integrated.imdb.service.MovieService;
import com.integrated.imdb.service.SuggestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@Tag(name = "Movie API", description = "Endpoints for movie discovery and recommendations")
public class MovieController {

    private static final Logger log = LoggerFactory.getLogger(MovieController.class);
    private final MovieService movieService;
    private final SuggestService suggestService;

    public MovieController(MovieService movieService, SuggestService suggestService) {
        this.movieService = movieService;
        this.suggestService = suggestService;
    }

    @GetMapping("/top-by-actor")
    @Operation(summary = "Get top movies by actor", 
              description = "Returns top rated movies for a specific actor")
    public ResponseEntity<List<MovieDto>> getTopMoviesByActor(
            @RequestParam String actor,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching top {} movies for actor: {}", limit, actor);
        return ResponseEntity.ok(movieService.getTopMoviesByActor(actor, limit));
    }

    @GetMapping("/search")
    @Operation(summary = "Search movies by title", 
              description = "Searches movies by title with partial matching")
    public ResponseEntity<List<MovieDto>> searchMovies(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Searching movies with query: {}", query);
        return ResponseEntity.ok(movieService.searchMoviesByTitle(query, limit));
    }

    @GetMapping("/top-rated")
    @Operation(summary = "Get top rated movies", 
              description = "Returns highest rated movies by average rating")
    public ResponseEntity<List<MovieDto>> getTopRatedMovies(
            @RequestParam(defaultValue = "20") int limit) {
        log.info("Fetching top {} rated movies", limit);
        return ResponseEntity.ok(movieService.getTopRatedMovies(limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get movie details", 
              description = "Returns detailed information about a specific movie")
    public ResponseEntity<MovieDto> getMovieDetails(@PathVariable String id) {
        log.info("Fetching details for movie: {}", id);
        MovieDto movie = movieService.getMovieById(id);
        if (movie != null) {
            return ResponseEntity.ok(movie);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter movies", 
              description = "Filters movies by multiple criteria")
    public ResponseEntity<List<MovieDto>> filterMovies(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String fromYear,
            @RequestParam(required = false) String toYear,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("Filtering movies with criteria - actor: {}, genre: {}, years: {}-{}", 
                actor, genre, fromYear, toYear);
        return ResponseEntity.ok(movieService.filterMovies(actor, genre, fromYear, toYear, limit));
    }

    @PostMapping("/suggest/start")
    @Operation(summary = "Start recommendation session", 
              description = "Starts a new recommendation session based on initial query")
    public ResponseEntity<SuggestResponse> startSuggestion(@RequestBody StartRequest request) {
        log.info("Starting recommendation session for query: {}", request.getQuery());
        return ResponseEntity.ok(suggestService.startSession(request));
    }

    @PostMapping("/suggest/feedback")
    @Operation(summary = "Submit feedback on recommendations", 
              description = "Submits user feedback on recommended movies")
    public ResponseEntity<SuggestResponse> submitFeedback(@RequestBody FeedbackRequest request) {
        log.info("Processing feedback for user: {}", request.getUserId());
        return ResponseEntity.ok(suggestService.recordFeedback(request));
    }

    @GetMapping("/suggest/{userId}")
    @Operation(summary = "Get recommendations", 
              description = "Gets personalized movie recommendations for a user")
    public ResponseEntity<SuggestResponse> getRecommendations(@PathVariable String userId) {
        log.info("Fetching recommendations for user: {}", userId);
        return ResponseEntity.ok(suggestService.getRecommendations(userId));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", 
              description = "Checks if the service is running")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Movie API",
            "version", "1.0.0"
        ));
    }
}

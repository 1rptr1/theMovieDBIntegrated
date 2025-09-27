package com.integrated.imdb.service;

import com.integrated.imdb.dto.FeedbackRequest;
import com.integrated.imdb.dto.MovieDto;
import com.integrated.imdb.dto.SuggestResponse;
import com.integrated.imdb.dto.StartRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SuggestService {
    
    private static final Logger log = LoggerFactory.getLogger(SuggestService.class);
    
    private final MovieService movieService;
    private final JdbcTemplate jdbcTemplate;

    public SuggestService(MovieService movieService, JdbcTemplate jdbcTemplate) {
        this.movieService = movieService;
        this.jdbcTemplate = jdbcTemplate;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            // Create user preferences table if not exists
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_preferences (
                    user_id VARCHAR(50) PRIMARY KEY,
                    preferences JSONB DEFAULT '{}'::jsonb,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Create user feedback table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_feedback (
                    id SERIAL PRIMARY KEY,
                    user_id VARCHAR(50) NOT NULL,
                    movie_id VARCHAR(20) NOT NULL,
                    liked BOOLEAN NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(user_id, movie_id)
                )
            """);
            
            // Create index for better query performance
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_user_feedback_user_id ON user_feedback(user_id);
                CREATE INDEX IF NOT EXISTS idx_user_feedback_movie_id ON user_feedback(movie_id);
            """);
            
            log.info("Database tables initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing database tables", e);
            throw new RuntimeException("Failed to initialize database tables", e);
        }
    }

    @Transactional
    public SuggestResponse startSession(StartRequest request) {
        String userId = request.getUserId() != null ? request.getUserId() : "user_" + UUID.randomUUID();
        
        // Save initial query to preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("initialQuery", request.getQuery());
        prefs.put("preferredGenres", new ArrayList<>());
        prefs.put("preferredActors", new ArrayList<>());
        prefs.put("lastUpdated", new Date().toString());
        
        savePreferences(userId, prefs);
        
        // Get initial recommendations based on query
        List<MovieDto> recommendations = movieService.searchMoviesByTitle(request.getQuery(), 10);
        
        return new SuggestResponse(userId, recommendations);
    }
    
    @Transactional
    public SuggestResponse recordFeedback(FeedbackRequest request) {
        // Save liked movies
        if (request.getLikedMovieIds() != null && !request.getLikedMovieIds().isEmpty()) {
            for (String movieId : request.getLikedMovieIds()) {
                jdbcTemplate.update("""
                    INSERT INTO user_feedback (user_id, movie_id, liked) 
                    VALUES (?, ?, true)
                    ON CONFLICT (user_id, movie_id) 
                    DO UPDATE SET liked = EXCLUDED.liked, created_at = CURRENT_TIMESTAMP
                    """, 
                    request.getUserId(), movieId);
            }
        }
        
        // Save disliked movies
        if (request.getDislikedMovieIds() != null && !request.getDislikedMovieIds().isEmpty()) {
            for (String movieId : request.getDislikedMovieIds()) {
                jdbcTemplate.update("""
                    INSERT INTO user_feedback (user_id, movie_id, liked) 
                    VALUES (?, ?, false)
                    ON CONFLICT (user_id, movie_id) 
                    DO UPDATE SET liked = EXCLUDED.liked, created_at = CURRENT_TIMESTAMP
                    """, 
                    request.getUserId(), movieId);
            }
        }
        
        // Get updated recommendations based on feedback
        return getRecommendations(request.getUserId());
    }
    
    @Transactional(readOnly = true)
    public SuggestResponse getRecommendations(String userId) {
        // Get user preferences
        Map<String, Object> prefs = getPreferences(userId);
        
        // Get user's liked movies
        List<String> likedMovies = jdbcTemplate.queryForList(
            "SELECT movie_id FROM user_feedback WHERE user_id = ? AND liked = true", 
            String.class, userId);
            
        if (likedMovies.isEmpty()) {
            // If no preferences yet, return popular movies
            return new SuggestResponse(userId, movieService.getTopRatedMovies(10));
        }
        
        // Get movie details for liked movies to analyze preferences
        List<MovieDto> likedMovieDetails = likedMovies.stream()
            .map(movieService::getMovieById)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
            
        // Extract preferences
        Set<String> likedGenres = new HashSet<>();
        Set<String> likedActors = new HashSet<>();
        
        for (MovieDto movie : likedMovieDetails) {
            if (movie.getGenres() != null) {
                Arrays.stream(movie.getGenres().split(","))
                      .map(String::trim)
                      .filter(g -> !g.isEmpty())
                      .forEach(likedGenres::add);
            }
            if (movie.getCast() != null) {
                Arrays.stream(movie.getCast().split(","))
                      .map(String::trim)
                      .filter(a -> !a.isEmpty())
                      .limit(3) // Limit to top 3 actors per movie
                      .forEach(likedActors::add);
            }
        }
        
        // Update preferences
        prefs.put("preferredGenres", new ArrayList<>(likedGenres));
        prefs.put("preferredActors", new ArrayList<>(likedActors));
        prefs.put("lastUpdated", new Date().toString());
        savePreferences(userId, prefs);
        
        // Get recommendations based on preferences
        List<MovieDto> recommendations = getPersonalizedRecommendations(userId, likedGenres, likedActors, likedMovies);
        
        return new SuggestResponse(userId, recommendations);
    }
    
    /**
     * Get personalized movie recommendations based on user preferences.
     * 
     * @param userId The ID of the user
     * @param likedGenres Set of genres the user has shown interest in
     * @param likedActors Set of actors the user has shown interest in
     * @param likedMovies List of movie IDs the user has liked
     * @return List of recommended movies
     */
    private List<MovieDto> getPersonalizedRecommendations(String userId, 
                                                         Set<String> likedGenres, 
                                                         Set<String> likedActors,
                                                         List<String> likedMovies) {
        try {
            // If we have both genres and actors, we can make a more targeted query
            if (!likedGenres.isEmpty() && !likedActors.isEmpty()) {
                log.debug("Generating recommendations for user {} with {} liked genres and {} liked actors", 
                         userId, likedGenres.size(), likedActors.size());
                
                // Exclude already liked movies
                String excludeCondition = likedMovies.isEmpty() ? "" : 
                    " AND t.tconst NOT IN ('" + String.join("','", likedMovies) + "')";
                
                // Build the query with dynamic parameters
                String sql = """
                    WITH movie_scores AS (
                        SELECT 
                            t.tconst,
                            t.primaryTitle,
                            t.startYear,
                            t.genres,
                            r.averageRating,
                            r.numVotes,
                            -- Score based on genre matches
                            (SELECT COUNT(*) FROM unnest(string_to_array(?, ',')) as g 
                             WHERE t.genres ILIKE '%' || g || '%') as genre_score,
                            -- Score based on actor matches (simplified)
                            (CASE WHEN EXISTS (
                                SELECT 1 FROM title_principals tp 
                                JOIN name_basics n ON tp.nconst = n.nconst 
                                WHERE tp.tconst = t.tconst 
                                AND n.primaryName ~* ?
                            ) THEN 1 ELSE 0 END) as actor_score
                        FROM title_basics t
                        JOIN title_ratings r ON t.tconst = r.tconst
                        WHERE t.titleType = 'movie'
                        AND r.numVotes > 1000  -- Minimum votes threshold
                        """ + excludeCondition + """
                    )
                    SELECT 
                        ms.tconst, 
                        ms.primaryTitle, 
                        ms.startYear, 
                        ms.genres, 
                        ms.averageRating, 
                        ms.numVotes, 
                        COALESCE(tb.runtimeMinutes, 0) as runtimeMinutes
                    FROM movie_scores ms
                    LEFT JOIN title_basics tb ON ms.tconst = tb.tconst
                    WHERE ms.genre_score > 0 OR ms.actor_score > 0
                    ORDER BY (ms.genre_score * 2 + ms.actor_score * 3) * (ms.averageRating * 0.1) DESC
                    LIMIT ?
                    """;
                
                // Prepare query parameters
                List<Object> queryParams = new ArrayList<>();
                queryParams.add(String.join(",", likedGenres));  // Single string of comma-separated genres
                queryParams.add(String.join("|", likedActors));  // Regex pattern for actor names
                queryParams.add(20); // Limit results to 20
                
                log.debug("Executing recommendation query with params: {}", queryParams);
                
                // Execute the query with parameters
                List<MovieDto> results = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> {
                        try {
                            MovieDto movie = new MovieDto();
                            movie.setTconst(rs.getString("tconst"));
                            movie.setPrimaryTitle(rs.getString("primaryTitle"));
                            movie.setStartYear(rs.getString("startYear"));
                            movie.setGenres(rs.getString("genres"));
                            movie.setAverageRating(rs.getDouble("averageRating"));
                            movie.setNumVotes(rs.getInt("numVotes"));
                            
                            // Set runtime using the helper method
                            int runtimeMinutes = rs.getInt("runtimeMinutes");
                            movie.setRuntimeFromMinutes(runtimeMinutes);
                            
                            // Initialize other OMDb data
                            movie.setPlot("Plot not available");
                            movie.setPoster("");
                            movie.setDirector("");
                            movie.setCast("");
                            
                            return movie;
                        } catch (Exception e) {
                            log.error("Error mapping movie result: {}", e.getMessage(), e);
                            return null;
                        }
                    },
                    queryParams.toArray()
                );
                
                // Filter out any null results from the mapping process
                return results.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error generating personalized recommendations: {}", e.getMessage(), e);
        }
        
        // Fallback to top rated movies if we don't have enough preference data or an error occurs
        log.info("Falling back to top-rated movies for user {}", userId);
        return movieService.getTopRatedMovies(20);
    }
    
    private void savePreferences(String userId, Map<String, Object> preferences) {
        try {
            jdbcTemplate.update("""
                INSERT INTO user_preferences (user_id, preferences) 
                VALUES (?, ?::jsonb)
                ON CONFLICT (user_id) 
                DO UPDATE SET preferences = EXCLUDED.preferences, created_at = CURRENT_TIMESTAMP
                """,
                userId, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(preferences));
        } catch (Exception e) {
            log.error("Error saving preferences for user {}", userId, e);
            throw new RuntimeException("Failed to save user preferences", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getPreferences(String userId) {
        try {
            String json = jdbcTemplate.queryForObject(
                "SELECT preferences::text FROM user_preferences WHERE user_id = ?",
                String.class, userId);
            
            if (json != null) {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, Map.class);
            }
        } catch (Exception e) {
            log.warn("Error getting preferences for user {}", userId, e);
        }
        
        // Return default preferences if none found
        Map<String, Object> defaultPrefs = new HashMap<>();
        defaultPrefs.put("preferredGenres", new ArrayList<>());
        defaultPrefs.put("preferredActors", new ArrayList<>());
        defaultPrefs.put("lastUpdated", new Date().toString());
        return defaultPrefs;
    }
}

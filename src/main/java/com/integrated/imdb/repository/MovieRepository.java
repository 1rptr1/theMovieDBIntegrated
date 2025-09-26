package com.integrated.imdb.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class MovieRepository {

    private final JdbcTemplate jdbcTemplate;

    public MovieRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Find top movies by actor with ratings
     */
    public List<Map<String, Object>> findTopMoviesByActor(String actor, int limit) {
        String sql = """
            SELECT t.tconst, t.primaryTitle, t.startYear, t.genres,
                   n.primaryName AS actorName, r.averageRating, r.numVotes
            FROM title_basics t
            JOIN title_principals p ON t.tconst = p.tconst
            JOIN name_basics n ON p.nconst = n.nconst
            LEFT JOIN title_ratings r ON t.tconst = r.tconst
            WHERE n.primaryName ILIKE ? AND t.titleType = 'movie'
            ORDER BY r.averageRating DESC NULLS LAST
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, "%" + actor + "%", limit);
    }

    /**
     * Search movies by title
     */
    public List<Map<String, Object>> searchMoviesByTitle(String title, int limit) {
        String sql = """
            SELECT t.tconst, t.primaryTitle, t.startYear, t.genres, 
                   r.averageRating, r.numVotes
            FROM title_basics t
            LEFT JOIN title_ratings r ON t.tconst = r.tconst
            WHERE t.primaryTitle ILIKE ? AND t.titleType = 'movie'
            ORDER BY r.averageRating DESC NULLS LAST
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, "%" + title + "%", limit);
    }

    /**
     * Get top rated movies
     */
    public List<Map<String, Object>> getTopRatedMovies(int limit) {
        String sql = """
            SELECT t.tconst, t.primaryTitle, t.startYear, t.genres, 
                   r.averageRating, r.numVotes
            FROM title_basics t
            JOIN title_ratings r ON t.tconst = r.tconst
            WHERE t.titleType = 'movie' AND r.numVotes > 10000
            ORDER BY r.averageRating DESC
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, limit);
    }

    /**
     * Get movie details by ID
     */
    public Map<String, Object> findMovieById(String tconst) {
        String sql = """
            SELECT t.tconst, t.primaryTitle, t.originalTitle, t.startYear, 
                   t.runtimeMinutes, t.genres, r.averageRating, r.numVotes
            FROM title_basics t
            LEFT JOIN title_ratings r ON t.tconst = r.tconst
            WHERE t.tconst = ?
            """;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, tconst);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Filter movies with multiple criteria
     */
    public List<Map<String, Object>> filterMovies(String actor, String genre, String fromYear, String toYear, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT t.tconst, t.primaryTitle, t.startYear, t.genres, 
                   r.averageRating, r.numVotes
            FROM title_basics t
            LEFT JOIN title_ratings r ON t.tconst = r.tconst
            """);

        boolean hasActor = actor != null && !actor.trim().isEmpty();
        if (hasActor) {
            sql.append("""
                JOIN title_principals p ON t.tconst = p.tconst
                JOIN name_basics n ON p.nconst = n.nconst
                """);
        }

        sql.append("WHERE t.titleType = 'movie' ");

        if (hasActor) {
            sql.append("AND n.primaryName ILIKE ? ");
        }
        if (genre != null && !genre.trim().isEmpty()) {
            sql.append("AND t.genres ILIKE ? ");
        }
        if (fromYear != null && !fromYear.trim().isEmpty()) {
            sql.append("AND t.startYear >= ? ");
        }
        if (toYear != null && !toYear.trim().isEmpty()) {
            sql.append("AND t.startYear <= ? ");
        }

        sql.append("ORDER BY r.averageRating DESC NULLS LAST LIMIT ?");

        // Build parameters array
        Object[] params = buildFilterParams(actor, genre, fromYear, toYear, limit);
        
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private Object[] buildFilterParams(String actor, String genre, String fromYear, String toYear, int limit) {
        java.util.List<Object> params = new java.util.ArrayList<>();
        
        if (actor != null && !actor.trim().isEmpty()) {
            params.add("%" + actor + "%");
        }
        if (genre != null && !genre.trim().isEmpty()) {
            params.add("%" + genre + "%");
        }
        if (fromYear != null && !fromYear.trim().isEmpty()) {
            params.add(fromYear);
        }
        if (toYear != null && !toYear.trim().isEmpty()) {
            params.add(toYear);
        }
        params.add(limit);
        
        return params.toArray();
    }

    /**
     * Get cast and crew for a movie
     */
    public List<Map<String, Object>> getMovieCastAndCrew(String tconst) {
        String sql = """
            SELECT n.primaryName, p.category, p.job, p.characters
            FROM title_principals p
            JOIN name_basics n ON p.nconst = n.nconst
            WHERE p.tconst = ?
            ORDER BY p.ordering
            """;
        return jdbcTemplate.queryForList(sql, tconst);
    }
}

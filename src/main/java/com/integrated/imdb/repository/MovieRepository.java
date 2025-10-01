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
            SELECT t.tconst, t.primary_title as "primaryTitle", t.start_year as "startYear", t.genres,
                   n.primary_name AS "actorName", r.average_rating as "averageRating", r.num_votes as "numVotes"
            FROM title_basics t
            JOIN title_principals p ON t.tconst = p.tconst
            JOIN name_basics n ON p.nconst = n.nconst
            LEFT JOIN title_ratings r ON t.tconst = r.tconst
            WHERE n.primary_name ILIKE ? AND t.title_type = 'movie'
            ORDER BY r.average_rating DESC NULLS LAST
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, "%" + actor + "%", limit);
    }

    /**
     * Search movies by title
     */
    public List<Map<String, Object>> searchMoviesByTitle(String title, int limit) {
        String sql = """
            SELECT t.tconst, t.primary_title as "primaryTitle", t.start_year as "startYear", t.genres, 
                   r.average_rating as "averageRating", r.num_votes as "numVotes"
            FROM title_basics t
            LEFT JOIN title_ratings r ON t.tconst = r.tconst
            WHERE t.primary_title ILIKE ? AND t.title_type = 'movie'
            ORDER BY r.average_rating DESC NULLS LAST
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, "%" + title + "%", limit);
    }

    /**
     * Get top rated movies with a minimum number of votes
     * 
     * @param limit Maximum number of movies to return
     * @param minVotes Minimum number of votes a movie must have to be included
     * @return List of top rated movies matching the criteria
     */
    public List<Map<String, Object>> getTopRatedMovies(int limit, int minVotes) {
        String sql = """
            SELECT t.tconst, t.primary_title as "primaryTitle", t.start_year as "startYear", t.genres, 
                   r.average_rating as "averageRating", r.num_votes as "numVotes", t.runtime_minutes as "runtimeMinutes"
            FROM title_basics t
            JOIN title_ratings r ON t.tconst = r.tconst
            WHERE t.title_type = 'movie' 
            AND r.num_votes >= ?
            ORDER BY r.average_rating DESC, r.num_votes DESC
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, minVotes, limit);
    }

    /**
     * Get movie details by ID
     */
    public Map<String, Object> findMovieById(String tconst) {
        String sql = """
            SELECT t.tconst, t.primary_title as "primaryTitle", t.original_title as "originalTitle", t.start_year as "startYear", 
                   t.runtime_minutes as "runtimeMinutes", t.genres, r.average_rating as "averageRating", r.num_votes as "numVotes"
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
            SELECT DISTINCT t.tconst, t.primary_title as "primaryTitle", t.start_year as "startYear", t.genres, 
                   r.average_rating as "averageRating", r.num_votes as "numVotes"
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

        sql.append("WHERE t.title_type = 'movie' ");

        if (hasActor) {
            sql.append("AND n.primary_name ILIKE ? ");
        }
        if (genre != null && !genre.trim().isEmpty()) {
            sql.append("AND t.genres ILIKE ? ");
        }
        if (fromYear != null && !fromYear.trim().isEmpty()) {
            sql.append("AND t.start_year >= ? ");
        }
        if (toYear != null && !toYear.trim().isEmpty()) {
            sql.append("AND t.start_year <= ? ");
        }

        sql.append("ORDER BY r.average_rating DESC NULLS LAST LIMIT ?");

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
            SELECT n.primary_name as "primaryName", p.category, p.job, p.characters
            FROM title_principals p
            JOIN name_basics n ON p.nconst = n.nconst
            WHERE p.tconst = ?
            ORDER BY p.ordering
            """;
        return jdbcTemplate.queryForList(sql, tconst);
    }
}

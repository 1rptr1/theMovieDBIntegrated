package com.integrated.imdb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DatabaseHealthCheck implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthCheck.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            // Check if we can query the database
            Map<String, Object> result = jdbcTemplate.queryForMap("SELECT 1 as status");
            
            // Check if we can access the title_basics table
            Long movieCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM title_basics WHERE title_type = 'movie' LIMIT 1", 
                Long.class);
                
            // Check if we can access the title_ratings table
            Long ratingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM title_ratings LIMIT 1", 
                Long.class);
            
            return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("movie_count", movieCount)
                .withDetail("rating_count", ratingCount)
                .build();
                
        } catch (DataAccessException e) {
            log.error("Database health check failed: {}", e.getMessage());
            return Health.down(e)
                .withDetail("error", e.getMessage())
                .withDetail("database", "PostgreSQL")
                .build();
        } catch (Exception e) {
            log.error("Unexpected error during database health check: {}", e.getMessage());
            return Health.outOfService()
                .withException(e)
                .withDetail("database", "PostgreSQL")
                .build();
        }
    }
    
    /**
     * Performs a more thorough check of the database schema
     */
    public Health checkSchema() {
        try {
            // Check if all required tables exist
            String[] requiredTables = {"title_basics", "title_ratings", "title_principals", "name_basics"};
            
            for (String table : requiredTables) {
                String sql = String.format("SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = '%s')", table);
                Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class);
                
                if (exists == null || !exists) {
                    return Health.down()
                        .withDetail("error", "Missing required table: " + table)
                        .withDetail("database", "PostgreSQL")
                        .build();
                }
            }
            
            return Health.up()
                .withDetail("message", "All required tables exist")
                .withDetail("database", "PostgreSQL")
                .build();
                
        } catch (Exception e) {
            log.error("Schema validation failed: {}", e.getMessage());
            return Health.down(e)
                .withDetail("error", "Schema validation failed: " + e.getMessage())
                .withDetail("database", "PostgreSQL")
                .build();
        }
    }
}

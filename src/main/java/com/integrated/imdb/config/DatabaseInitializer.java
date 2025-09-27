package com.integrated.imdb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Component responsible for initializing the database with required schema and data.
 * This runs after the application context is loaded but before the application starts.
 */
@Component
@Profile("!test") // Don't run during tests
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    
    @Autowired
    private DataSource dataSource;
    private final DatabaseHealthCheck databaseHealthCheck;
    
    // List of SQL scripts to execute in order
    private static final List<String> SQL_SCRIPTS = Arrays.asList(
        "schema.sql",
        "data.sql"
    );
    
    @Autowired
    public DatabaseInitializer(DataSource dataSource, DatabaseHealthCheck databaseHealthCheck) {
        this.dataSource = dataSource;
        this.databaseHealthCheck = databaseHealthCheck;
    }
    
    @Override
    public void run(String... args) {
        log.info("Starting database initialization...");
        
        // First, check if database is accessible
        if (!isDatabaseAccessible()) {
            log.error("Database is not accessible. Please check your database connection settings.");
            return;
        }
        
        // Execute each SQL script
        for (String script : SQL_SCRIPTS) {
            executeSqlScript(script);
        }
        
        // Verify database health after initialization
        verifyDatabaseHealth();
        
        log.info("Database initialization completed successfully");
    }
    
    private boolean isDatabaseAccessible() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            log.error("Failed to connect to database: {}", e.getMessage());
            return false;
        }
    }
    
    private void executeSqlScript(String scriptName) {
        Resource resource = new ClassPathResource(scriptName);
        
        if (!resource.exists()) {
            log.warn("SQL script not found: {}", scriptName);
            return;
        }
        
        log.info("Executing SQL script: {}", scriptName);
        
        try (Connection conn = dataSource.getConnection()) {
            // Disable auto-commit to run the entire script in a transaction
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                ScriptUtils.executeSqlScript(conn, resource);
                conn.commit();
                log.info("Successfully executed SQL script: {}", scriptName);
            } catch (Exception e) {
                conn.rollback();
                log.error("Failed to execute SQL script: {}", scriptName, e);
                throw new RuntimeException("Failed to execute SQL script: " + scriptName, e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            log.error("Database error while executing script: {}", scriptName, e);
            throw new RuntimeException("Database error while executing script: " + scriptName, e);
        }
    }
    
    private void verifyDatabaseHealth() {
        log.info("Verifying database health...");
        
        // Check basic database health
        Health dbHealth = databaseHealthCheck.health();
        log.info("Database health status: {}", dbHealth.getStatus());
        
        // Check schema health
        Health schemaHealth = databaseHealthCheck.checkSchema();
        log.info("Database schema health status: {}", schemaHealth.getStatus());
        
        if (dbHealth.getStatus() != Status.UP || schemaHealth.getStatus() != Status.UP) {
            log.warn("Database health check failed. Some features may not work as expected.");
            log.warn("Database health details: {}", dbHealth.getDetails());
            log.warn("Schema health details: {}", schemaHealth.getDetails());
        } else {
            log.info("Database health check passed successfully");
        }
    }
}

package com.integrated.imdb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for database migrations and schema updates.
 * Looks for SQL migration files in the classpath under db/migration/
 * and applies them in order based on their version numbers.
 */
@Service
public class DatabaseMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationService.class);
    private static final Pattern MIGRATION_FILE_PATTERN = 
        Pattern.compile("V(\\d+)__(.+)\\.sql");
    
    private final DataSource dataSource;
    private final boolean enabled;
    
    @Autowired
    public DatabaseMigrationService(
            DataSource dataSource,
            @Value("${app.database.migration.enabled:true}") boolean enabled) {
        this.dataSource = dataSource;
        this.enabled = enabled;
    }
    
    @PostConstruct
    public void migrate() {
        if (!enabled) {
            log.info("Database migration is disabled");
            return;
        }
        
        try {
            log.info("Starting database migration...");
            
            // Create migrations table if it doesn't exist
            createMigrationsTable();
            
            // Get all applied migrations
            List<String> appliedMigrations = getAppliedMigrations();
            log.info("Found {} applied migrations", appliedMigrations.size());
            
            // Find and apply new migrations
            List<MigrationFile> migrationFiles = findMigrationFiles();
            log.info("Found {} migration files", migrationFiles.size());
            
            int appliedCount = 0;
            for (MigrationFile migration : migrationFiles) {
                if (!appliedMigrations.contains(migration.getVersion())) {
                    log.info("Applying migration: {}", migration.getFilename());
                    applyMigration(migration);
                    recordMigration(migration);
                    appliedCount++;
                }
            }
            
            log.info("Database migration completed. Applied {} new migrations.", appliedCount);
            
        } catch (Exception e) {
            log.error("Database migration failed", e);
            throw new RuntimeException("Database migration failed", e);
        }
    }
    
    private void createMigrationsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS schema_migrations (
                version VARCHAR(50) PRIMARY KEY,
                description VARCHAR(200) NOT NULL,
                script_name VARCHAR(255) NOT NULL,
                checksum VARCHAR(32) NOT NULL,
                installed_by VARCHAR(100) NOT NULL,
                installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                execution_time INTEGER NOT NULL,
                success BOOLEAN NOT NULL
            )
            """;
            
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create migrations table", e);
        }
    }
    
    private List<String> getAppliedMigrations() {
        String sql = "SELECT version FROM schema_migrations WHERE success = true ORDER BY version";
        try (Connection conn = dataSource.getConnection()) {
            return conn.createStatement()
                .executeQuery(sql)
                .getStatement()
                .getResultSet()
                .getString(1);
        } catch (SQLException e) {
            log.warn("Failed to get applied migrations: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private List<MigrationFile> findMigrationFiles() throws IOException {
        Resource[] resources = new ClassPathResource("db/migration/").getResources("V*__.sql");
        
        return Arrays.stream(resources)
            .map(resource -> {
                String filename = resource.getFilename();
                Matcher matcher = MIGRATION_FILE_PATTERN.matcher(filename);
                if (matcher.matches()) {
                    return new MigrationFile(
                        matcher.group(1),  // version
                        matcher.group(2).replace("_", " "),  // description
                        filename,
                        resource
                    );
                }
                return null;
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(MigrationFile::getVersion))
            .collect(Collectors.toList());
    }
    
    @Transactional
    protected void applyMigration(MigrationFile migration) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        
        try (Connection conn = dataSource.getConnection()) {
            // Start transaction
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                // Execute migration script
                ScriptUtils.executeSqlScript(
                    conn,
                    new EncodedResource(migration.getResource(), StandardCharsets.UTF_8)
                );
                
                // Commit transaction
                conn.commit();
                success = true;
                
            } catch (Exception e) {
                // Rollback on error
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
            
        } catch (Exception e) {
            log.error("Failed to apply migration: {}", migration.getFilename(), e);
            success = false;
            throw new RuntimeException("Migration failed: " + migration.getFilename(), e);
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logMigration(migration, success, executionTime);
        }
    }
    
    private void recordMigration(MigrationFile migration) {
        String sql = """
            INSERT INTO schema_migrations 
                (version, description, script_name, checksum, installed_by, execution_time, success)
            VALUES (?, ?, ?, ?, current_user, ?, true)
            """;
            
        try (Connection conn = dataSource.getConnection()) {
            conn.prepareStatement(sql)
                .setString(1, migration.getVersion())
                .setString(2, migration.getDescription())
                .setString(3, migration.getFilename())
                .setString(4, "") // Checksum could be calculated here
                .setLong(5, 0) // Execution time not available here
                .executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to record migration: {}", migration.getFilename(), e);
        }
    }
    
    private void logMigration(MigrationFile migration, boolean success, long executionTime) {
        String status = success ? "SUCCESS" : "FAILED";
        log.info("Migration {} {} in {} ms", 
            migration.getFilename(), status, executionTime);
    }
    
    /**
     * Represents a migration file
     */
    private static class MigrationFile {
        private final String version;
        private final String description;
        private final String filename;
        private final Resource resource;
        
        public MigrationFile(String version, String description, String filename, Resource resource) {
            this.version = version;
            this.description = description;
            this.filename = filename;
            this.resource = resource;
        }
        
        public String getVersion() { return version; }
        public String getDescription() { return description; }
        public String getFilename() { return filename; }
        public Resource getResource() { return resource; }
    }
}

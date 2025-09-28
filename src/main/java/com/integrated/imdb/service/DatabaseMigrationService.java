package com.integrated.imdb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import com.integrated.imdb.model.MigrationFile;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
@Service
public class DatabaseMigrationService {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationService.class);
    private static final Pattern MIGRATION_FILE_PATTERN = Pattern.compile("^V(\\d+)__(.+)\\.sql$");
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DataSource dataSource;
    
    @PostConstruct
    public void init() {
        try {
            createMigrationsTableIfNotExists();
            migrate();
        } catch (Exception e) {
            log.error("Failed to run database migrations", e);
            throw new RuntimeException("Failed to run database migrations", e);
        }
    }
    
    private void createMigrationsTableIfNotExists() throws SQLException {
        try (java.sql.Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version VARCHAR(50) PRIMARY KEY,
                    description VARCHAR(200) NOT NULL,
                    script VARCHAR(1000) NOT NULL,
                    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    success BOOLEAN NOT NULL
                )
            """);
        }
    }
    
    public List<String> getAppliedMigrations() {
        try {
            return jdbcTemplate.queryForList(
                "SELECT version FROM schema_version ORDER BY version",
                String.class
            );
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private List<MigrationFile> findMigrationFiles() throws IOException {
        List<MigrationFile> migrationFiles = new ArrayList<>();
        
        // Get all SQL files from the classpath
        java.nio.file.Path migrationsDir = java.nio.file.Paths.get("src/main/resources/db/migration");
        if (!java.nio.file.Files.exists(migrationsDir)) {
            log.warn("Migrations directory not found: {}", migrationsDir);
            return migrationFiles;
        }
        
        try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(migrationsDir)) {
            paths
                .filter(java.nio.file.Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".sql"))
                .forEach(path -> {
                    String filename = path.getFileName().toString();
                    Matcher matcher = MIGRATION_FILE_PATTERN.matcher(filename);
                    if (matcher.matches()) {
                        migrationFiles.add(new MigrationFile(
                            matcher.group(1),  // version
                            matcher.group(2).replace("_", " "),  // description
                            filename,
                            new ClassPathResource("db/migration/" + filename)
                        ));
                    }
                });
        }
        
        migrationFiles.sort(Comparator.comparing(MigrationFile::getVersion));
        return migrationFiles;
    }
    
    @Transactional
    public void migrate() throws SQLException, IOException {
        List<String> appliedMigrations = getAppliedMigrations();
        
        for (MigrationFile migration : findMigrationFiles()) {
            if (!appliedMigrations.contains(migration.getVersion())) {
                log.info("Applying migration: {} - {}", migration.getVersion(), migration.getDescription());
                applyMigration(migration);
            }
        }
    }
    
    private void applyMigration(MigrationFile migration) throws SQLException, IOException {
        String sql = StreamUtils.copyToString(
            migration.getResource().getInputStream(),
            StandardCharsets.UTF_8
        );
        
        long startTime = System.currentTimeMillis();
        boolean success = false;
        
        try (java.sql.Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Execute the migration script
            stmt.execute(sql);
            success = true;
            
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record the migration in the schema_version table
            jdbcTemplate.update("""
                INSERT INTO schema_version 
                    (version, description, script, success)
                VALUES (?, ?, ?, ?)
                """,
                migration.getVersion(),
                migration.getDescription(),
                migration.getFilename(),

                success
            );
            
            if (!success) {
                throw new SQLException("Migration failed: " + migration.getFilename());
            }
        }
    }
    
}

package com.integrated.imdb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                // Read the entire script content
                String scriptContent = readScriptContent(resource);
                
                // Split into statements, respecting dollar-quoted strings
                List<String> statements = splitSqlStatements(scriptContent);
                
                // Execute each statement
                try (Statement stmt = conn.createStatement()) {
                    for (int i = 0; i < statements.size(); i++) {
                        String sql = statements.get(i).trim();
                        if (!sql.isEmpty()) {
                            log.debug("Executing statement #{}: {}", i + 1, 
                                sql.length() > 100 ? sql.substring(0, 100) + "..." : sql);
                            stmt.execute(sql);
                        }
                    }
                }
                
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
    
    private String readScriptContent(Resource resource) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    private List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inDollarQuote = false;
        String dollarQuoteTag = null;
        boolean inMultiLineComment = false;
        
        String[] lines = script.split("\n");
        
        for (String line : lines) {
            // Handle single-line comments
            if (line.trim().startsWith("--")) {
                continue;
            }
            
            int i = 0;
            while (i < line.length()) {
                char c = line.charAt(i);
                
                // Check for multi-line comment start
                if (!inDollarQuote && i < line.length() - 1 && c == '/' && line.charAt(i + 1) == '*') {
                    inMultiLineComment = true;
                    i += 2;
                    continue;
                }
                
                // Check for multi-line comment end
                if (inMultiLineComment && i < line.length() - 1 && c == '*' && line.charAt(i + 1) == '/') {
                    inMultiLineComment = false;
                    i += 2;
                    continue;
                }
                
                // Skip if in comment
                if (inMultiLineComment) {
                    i++;
                    continue;
                }
                
                // Check for dollar quote
                if (c == '$') {
                    // Try to match a dollar quote tag
                    Pattern pattern = Pattern.compile("\\$([a-zA-Z_]*)\\$");
                    Matcher matcher = pattern.matcher(line.substring(i));
                    if (matcher.find() && matcher.start() == 0) {
                        String tag = matcher.group(0);
                        if (inDollarQuote) {
                            // Check if this closes the current dollar quote
                            if (tag.equals(dollarQuoteTag)) {
                                inDollarQuote = false;
                                dollarQuoteTag = null;
                            }
                        } else {
                            // Start a new dollar quote
                            inDollarQuote = true;
                            dollarQuoteTag = tag;
                        }
                        currentStatement.append(tag);
                        i += tag.length();
                        continue;
                    }
                }
                
                // Check for statement terminator (semicolon)
                if (!inDollarQuote && c == ';') {
                    currentStatement.append(c);
                    String stmt = currentStatement.toString().trim();
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    currentStatement = new StringBuilder();
                    i++;
                    continue;
                }
                
                // Add character to current statement
                currentStatement.append(c);
                i++;
            }
            
            // Add newline to preserve formatting
            currentStatement.append("\n");
        }
        
        // Add any remaining statement
        String stmt = currentStatement.toString().trim();
        if (!stmt.isEmpty()) {
            statements.add(stmt);
        }
        
        return statements;
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

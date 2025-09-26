package com.integrated.imdb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.initialization-mode:always}")
    private String initializationMode;

    @Value("${spring.datasource.platform:postgresql}")
    private String databasePlatform;

    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        log.info("Initializing database with platform: {}", databasePlatform);
        
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        
        // Only run schema and data scripts if initialization is enabled
        if ("always".equalsIgnoreCase(initializationMode)) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            
            // Load schema.sql first
            populator.addScript(new ClassPathResource("schema.sql"));
            log.info("Added schema.sql to database initialization");
            
            // Then load data.sql if it exists
            if (new ClassPathResource("data.sql").exists()) {
                populator.addScript(new ClassPathResource("data.sql"));
                log.info("Added data.sql to database initialization");
            }
            
            // Configure the populator
            populator.setSeparator(";");
            populator.setContinueOnError(true); // Continue on error to avoid startup failure
            
            initializer.setDatabasePopulator(populator);
            initializer.setEnabled(true);
            
            log.info("Database initialization configured");
        } else {
            log.info("Database initialization is disabled (initialization-mode={})", initializationMode);
            initializer.setEnabled(false);
        }
        
        return initializer;
    }
}

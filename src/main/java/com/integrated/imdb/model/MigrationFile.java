package com.integrated.imdb.model;

import org.springframework.core.io.Resource;

public class MigrationFile {
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
    
    public String getVersion() {
        return version;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public Resource getResource() {
        return resource;
    }
}

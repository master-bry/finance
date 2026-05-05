package com.master.finance.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@PropertySource("classpath:application.properties")
public class EnvConfig {
    
    @Autowired
    private ConfigurableEnvironment environment;
    
    private static Dotenv dotenv;
    
    @PostConstruct
    public void loadEnv() {
        try {
            // Load .env file from project root
            dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();
            
            // Create a map for environment variables
            Map<String, Object> envMap = new HashMap<>();
            
            // Add MongoDB configuration
            String mongoUri = getMongoUri();
            envMap.put("MONGODB_URI", mongoUri);
            envMap.put("MONGODB_DATABASE", getMongoDatabase());
            envMap.put("SERVER_PORT", getServerPort());
            
            // Add to Spring environment
            MapPropertySource propertySource = new MapPropertySource("dotenvProperties", envMap);
            environment.getPropertySources().addFirst(propertySource);
            
            // Set system properties for Spring Boot to use
            System.setProperty("MONGODB_URI", mongoUri);
            System.setProperty("MONGODB_DATABASE", getMongoDatabase());
            System.setProperty("SERVER_PORT", getServerPort());
            
            System.out.println("\n✅ Environment Configuration Loaded Successfully!");
            System.out.println("📁 MongoDB URI: " + maskUri(mongoUri));
            System.out.println("📁 Database: " + getMongoDatabase());
            System.out.println("📁 Server Port: " + getServerPort());
            System.out.println("📁 Raw URI Length: " + (mongoUri != null ? mongoUri.length() : "null"));
            System.out.println("📁 URI Starts with: " + (mongoUri != null && mongoUri.length() > 10 ? mongoUri.substring(0, 10) : "invalid"));
            System.out.println("========================================\n");
            
        } catch (Exception e) {
            System.out.println("⚠️ No .env file found, using default configuration");
            setDefaultValues();
        }
    }
    
    private String getMongoUri() {
        String uri = dotenv.get("MONGODB_URI");
        if (uri != null && !uri.isEmpty()) {
            return uri;
        }
        // Default to localhost for development (change to Atlas in production)
        return "mongodb://localhost:27017/finance-tracker";
    }
    
    private String getMongoDatabase() {
        String db = dotenv.get("MONGODB_DATABASE");
        if (db != null && !db.isEmpty()) {
            return db;
        }
        return "finance-tracker";
    }
    
    private String getServerPort() {
        String port = dotenv.get("SERVER_PORT");
        if (port != null && !port.isEmpty()) {
            return port;
        }
        return "8080";
    }
    
    private void setDefaultValues() {
        System.setProperty("MONGODB_URI", "mongodb://localhost:27017/finance-tracker");
        System.setProperty("MONGODB_DATABASE", "finance-tracker");
        System.setProperty("SERVER_PORT", "8080");
    }
    
    private String maskUri(String uri) {
        if (uri == null) return "Not set";
        return uri.replaceAll(":[^:@]+@", ":****@");
    }
}
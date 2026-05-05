package com.master.finance.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cache configuration for performance optimization
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Define cache regions for different data types
        cacheManager.setCacheNames(Arrays.asList(
            "transactions",      // User transactions
            "dashboard",         // Dashboard statistics
            "budgets",           // Budget data
            "debts",             // Debt information
            "investments",       // Investment data
            "goals",             // Goal data
            "reports",           // Report calculations
            "userStats"          // User statistics
        ));
        
        // Allow null values in cache
        cacheManager.setAllowNullValues(true);
        
        return cacheManager;
    }
}

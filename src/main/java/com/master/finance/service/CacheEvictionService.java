package com.master.finance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CacheEvictionService {

    @Autowired
    private CacheManager cacheManager;

    @Scheduled(fixedRate = 21600000)
    public void evictExchangeRatesCache() {
        org.springframework.cache.Cache cache = cacheManager.getCache("exchangeRates");
        if (cache != null) cache.clear();
    }

    @Scheduled(fixedRate = 3600000)
    public void evictUsersCache() {
        org.springframework.cache.Cache cache = cacheManager.getCache("users");
        if (cache != null) cache.clear();
    }
}
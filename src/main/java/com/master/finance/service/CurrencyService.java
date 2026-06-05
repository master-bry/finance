package com.master.finance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);
    private static final String API_URL = "https://open.er-api.com/v6/latest/TZS";

    public double convert(double amount, String targetCurrency) {
        if ("TZS".equals(targetCurrency)) return amount;
        double rate = getExchangeRate(targetCurrency);
        return amount * rate;
    }

    
    @Cacheable(value = "exchangeRates", key = "#targetCurrency")
public double getExchangeRate(String targetCurrency) {
    if ("TZS".equals(targetCurrency)) return 1.0;
    try {
        RestTemplate rest = new RestTemplate();
        Map<?, ?> response = rest.getForObject(API_URL, Map.class);
        if (response != null && response.containsKey("rates")) {
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            if (rates.containsKey(targetCurrency)) {
                return ((Number) rates.get(targetCurrency)).doubleValue();
            }
        }
    } catch (Exception e) {
        log.error("Failed to fetch exchange rate for {}: {}", targetCurrency, e.getMessage());
    }
    return 1.0;
}
}

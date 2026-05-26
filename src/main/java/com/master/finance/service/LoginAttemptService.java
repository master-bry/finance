package com.master.finance.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPTS = 5;
    private final long LOCKOUT_DURATION_MINUTES = 15;

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public void loginFailed(String username) {
        LoginAttempt attempt = attempts.computeIfAbsent(username.toLowerCase(), k -> new LoginAttempt());
        attempt.increment();
        attempt.setLastAttempt(LocalDateTime.now());
    }

    public boolean isBlocked(String username) {
        LoginAttempt attempt = attempts.get(username.toLowerCase());
        if (attempt == null) return false;
        if (attempt.getCount() < MAX_ATTEMPTS) return false;
        if (attempt.getLastAttempt().plusMinutes(LOCKOUT_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
            attempts.remove(username.toLowerCase());
            return false;
        }
        return true;
    }

    public int getRemainingAttempts(String username) {
        LoginAttempt attempt = attempts.get(username.toLowerCase());
        if (attempt == null) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - attempt.getCount());
    }

    public void loginSucceeded(String username) {
        attempts.remove(username.toLowerCase());
    }

    private static class LoginAttempt {
        private int count = 0;
        private LocalDateTime lastAttempt;

        void increment() { count++; }
        int getCount() { return count; }
        LocalDateTime getLastAttempt() { return lastAttempt; }
        void setLastAttempt(LocalDateTime lastAttempt) { this.lastAttempt = lastAttempt; }
    }
}

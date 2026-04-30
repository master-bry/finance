package com.master.finance.service;

import com.master.finance.model.User;
import com.master.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setEnabled(true);
        user.setDeleted(false);
        user.setRole("USER");
        return userRepository.save(user);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @CacheEvict(value = "users", key = "#username")
    public void updateLastLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }
    
    public void addNotification(String userId, String notification) {
        userRepository.findById(userId).ifPresent(user -> {
            user.getNotifications().add(notification);
            userRepository.save(user);
        });
    }
    
    public void addNotifications(String userId, List<String> notifications) {
        userRepository.findById(userId).ifPresent(user -> {
            user.getNotifications().addAll(notifications);
            userRepository.save(user);
        });
    }
    
    public List<String> getNotifications(String userId) {
        return userRepository.findById(userId)
                .map(User::getNotifications)
                .orElse(List.of());
    }
    
    public void clearNotifications(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.getNotifications().clear();
            userRepository.save(user);
        });
    }
    
    @CacheEvict(value = "users", key = "#userId")
    public User updateProfile(String userId, User updatedUser) {
        return userRepository.findById(userId).map(user -> {
            user.setFullName(updatedUser.getFullName());
            user.setPhoneNumber(updatedUser.getPhoneNumber());
            user.setCurrency(updatedUser.getCurrency());
            return userRepository.save(user);
        }).orElseThrow();
    }
    
    public void changePassword(String userId, String newPassword) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        });
    }
    
    public void softDeleteUser(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setDeleted(true);
            user.setDeletedAt(LocalDateTime.now());
            user.setEnabled(false);
            userRepository.save(user);
        });
    }
    
    public void enableUser(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setEnabled(true);
            userRepository.save(user);
        });
    }
    
    public void disableUser(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setEnabled(false);
            userRepository.save(user);
        });
    }
    
    public List<User> getAllActiveUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isDeleted() && user.isEnabled())
                .toList();
    }
    
    public long getTotalUsersCount() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .count();
    }
}
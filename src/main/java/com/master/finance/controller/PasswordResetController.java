package com.master.finance.controller;

import com.master.finance.model.User;
import com.master.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reset-password")
public class PasswordResetController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/{email}/{newPassword}")
    public String resetPassword(@PathVariable String email, @PathVariable String newPassword) {
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return "❌ User with email " + email + " not found!";
        }
        
        // Encode the new password
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        return "✅ Password reset successfully!\n\n" +
               "Email: " + email + "\n" +
               "New Password: " + newPassword + "\n\n" +
               "You can now login with these credentials!";
    }
    
    @GetMapping("/check-user/{email}")
    public String checkUser(@PathVariable String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return "❌ User not found!";
        }
        
        return "✅ User found!\n" +
               "Username: " + user.getUsername() + "\n" +
               "Email: " + user.getEmail() + "\n" +
               "Full Name: " + user.getFullName() + "\n" +
               "Enabled: " + user.isEnabled();
    }
}
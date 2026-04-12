package com.master.finance.controller;

import com.master.finance.model.User;
import com.master.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugAuthController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/debug/login")
    public String debugLogin(@RequestParam String username, @RequestParam String password) {
        // Find user by username or email
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = userRepository.findByEmail(username).orElse(null);
        }
        
        if (user == null) {
            return "❌ User not found with username/email: " + username;
        }
        
        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
        
        StringBuilder result = new StringBuilder();
        result.append("=== DEBUG INFORMATION ===\n\n");
        result.append("Username: ").append(user.getUsername()).append("\n");
        result.append("Email: ").append(user.getEmail()).append("\n");
        result.append("Enabled: ").append(user.isEnabled()).append("\n");
        result.append("Deleted: ").append(user.isDeleted()).append("\n");
        result.append("Password in DB: ").append(user.getPassword()).append("\n");
        result.append("Password provided: ").append(password).append("\n");
        result.append("Password matches: ").append(passwordMatches ? "✅ YES" : "❌ NO").append("\n");
        
        if (!passwordMatches) {
            result.append("\n⚠️ Password doesn't match! The hash in DB is for a different password.\n");
        }
        
        return result.toString();
    }
    
    @GetMapping("/update-my-password")
    public String updateMyPassword() {
        // Find your user
        User user = userRepository.findByEmail("5326bry@gmail.com").orElse(null);
        
        if (user == null) {
            user = userRepository.findByUsername("5326bry@gmail.com").orElse(null);
        }
        
        if (user == null) {
            return "❌ User not found!";
        }
        
        // Generate NEW password hash using your app's encoder
        String newPassword = "Admin@2022";
        String newHash = passwordEncoder.encode(newPassword);
        user.setPassword(newHash);
        userRepository.save(user);
        
        return "✅ PASSWORD UPDATED SUCCESSFULLY!\n\n" +
               "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
               "Username: " + user.getUsername() + "\n" +
               "Password: " + newPassword + "\n" +
               "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
               "New Hash stored in DB:\n" + newHash + "\n\n" +
               "🔐 You can now login with:\n" +
               "   Username: " + user.getUsername() + "\n" +
               "   Password: " + newPassword + "\n\n" +
               "👉 Go to: http://localhost:8080/login\n" +
               "👉 Or test with curl:\n" +
               "   curl -X POST http://localhost:8080/login \\\n" +
               "     -d \"username=" + user.getUsername() + "\" \\\n" +
               "     -d \"password=" + newPassword + "\"";
    }
    
    @GetMapping("/generate-hash")
    public String generateHash(@RequestParam String password) {
        String hash = passwordEncoder.encode(password);
        
        return "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
               "🔐 PASSWORD HASH GENERATOR\n" +
               "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
               "Original Password: " + password + "\n" +
               "Generated Hash: " + hash + "\n" +
               "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
               "MongoDB Update Command:\n" +
               "db.users.updateOne(\n" +
               "  { username: \"YOUR_USERNAME\" },\n" +
               "  { $set: { password: \"" + hash + "\" } }\n" +
               ")";
    }
}
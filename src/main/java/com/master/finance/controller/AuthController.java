package com.master.finance.controller;

import com.master.finance.model.User;
import com.master.finance.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registered", required = false) String registered,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
            log.warn("Login failed attempt");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        if (registered != null) {
            model.addAttribute("message", "Registration successful! Please login.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(User user, RedirectAttributes redirectAttributes) {
        try {
            log.info("Attempting to register user: {}", user.getUsername());
            
            // Validation
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Username is required");
                redirectAttributes.addFlashAttribute("user", user);
                return "redirect:/register";
            }
            
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email is required");
                redirectAttributes.addFlashAttribute("user", user);
                return "redirect:/register";
            }
            
            if (user.getPassword() == null || user.getPassword().length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters");
                redirectAttributes.addFlashAttribute("user", user);
                return "redirect:/register";
            }
            
            // Check if user exists
            if (userService.existsByUsername(user.getUsername())) {
                redirectAttributes.addFlashAttribute("error", "Username already exists");
                redirectAttributes.addFlashAttribute("user", user);
                return "redirect:/register";
            }

            if (userService.existsByEmail(user.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Email already registered");
                redirectAttributes.addFlashAttribute("user", user);
                return "redirect:/register";
            }

            // Register user
            userService.registerUser(user);
            log.info("User registered successfully: {}", user.getUsername());
            
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login?registered=true";
            
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/register";
        }
    }

    @GetMapping("/auth/logout")
    public String logoutSuccess(Model model) {
        model.addAttribute("title", "Logged Out");
        return "auth/logout";
    }
}
package com.master.finance.controller;

import com.master.finance.model.User;
import com.master.finance.service.AuditService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String showProfile(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("profileUser", user);
        model.addAttribute("currentPage", "profile");
        model.addAttribute("pageSubtitle", "Manage your account settings");
        model.addAttribute("title", "Profile Settings");

        String[] currencies = {"TZS", "USD", "EUR", "GBP", "KES", "UGX", "RWF", "ZAR", "NGN", "GHS"};
        model.addAttribute("currencies", currencies);

        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfile(Authentication authentication,
                                @RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String phoneNumber,
                                @RequestParam(required = false) String currency,
                                RedirectAttributes redirectAttributes) {
        String userId = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();

        User updatedUser = new User();
        updatedUser.setFullName(fullName);
        updatedUser.setPhoneNumber(phoneNumber);
        if (currency != null) updatedUser.setCurrency(currency);

        userService.updateProfile(userId, updatedUser);
        auditService.logAsync(userId, "PROFILE_UPDATE", "User", userId, "Profile updated");
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully");

        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(Authentication authentication,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        String userId = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();

        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/profile";
        }

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters");
            return "redirect:/profile";
        }

        userService.changePassword(userId, newPassword);
        auditService.logAsync(userId, "PASSWORD_CHANGE", "User", userId, "Password changed");
        redirectAttributes.addFlashAttribute("success", "Password changed successfully");

        return "redirect:/profile";
    }
}

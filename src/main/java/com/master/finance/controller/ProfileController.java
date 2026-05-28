package com.master.finance.controller;

import com.master.finance.model.User;
import com.master.finance.service.AuditService;
import com.master.finance.service.UserService;
import com.master.finance.utils.TotpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

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

        String[] currencies = {"TZS", "USD", "EUR", "GBP", "KES", "UGX", "RWF", "ZAR", "NGN", "GHS", "JPY", "CNY", "INR", "BRL", "MXN", "CAD", "AUD", "CHF", "SEK", "NOK", "DKK", "AED", "SAR", "TRY", "PKR", "BDT", "LKR", "NPR", "ETB", "MAD", "EGP", "NGN", "GHS", "ZAR", "KES", "UGX", "RWF", "BIF", "SSP"};
        model.addAttribute("currencies", currencies);

        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfile(Authentication authentication,
                                @RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String phoneNumber,
                                @RequestParam(required = false) String currency,
                                @RequestParam("profilePhoto") MultipartFile profilePhoto,
                                RedirectAttributes redirectAttributes) {
        String userId = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();

        User updatedUser = new User();
        updatedUser.setFullName(fullName);
        updatedUser.setPhoneNumber(phoneNumber);
        if (currency != null) updatedUser.setCurrency(currency);

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            try {
                String contentType = profilePhoto.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "Only image files are allowed");
                    return "redirect:/profile";
                }
                byte[] bytes = profilePhoto.getBytes();
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                updatedUser.setProfilePhoto("data:" + contentType + ";base64," + base64);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Failed to upload profile photo");
                return "redirect:/profile";
            }
        }

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
                                 HttpServletRequest request,
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

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        redirectAttributes.addFlashAttribute("success", "Password changed. Please login with your new password.");
        return "redirect:/login";
    }

    @GetMapping("/setup-2fa")
    public String showSetup2fa(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isTotpEnabled()) {
            model.addAttribute("totpEnabled", true);
        } else {
            String secret = TotpUtil.generateSecret();
            String uri = TotpUtil.getProvisioningUri(secret, user.getEmail(), "FinanceTracker");
            model.addAttribute("secret", secret);
            model.addAttribute("qrUri", uri);

            user.setTotpSecret(secret);
            userService.save(user);
        }
        model.addAttribute("currentPage", "profile");
        return "profile/setup-2fa";
    }

    @PostMapping("/verify-2fa")
    public String verifyAndEnable2fa(Authentication authentication,
                                     @RequestParam String code,
                                     RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (code == null || !code.matches("\\d{6}")) {
            redirectAttributes.addFlashAttribute("error", "Invalid code format. Please enter a 6-digit code.");
            return "redirect:/profile/setup-2fa";
        }

        if (TotpUtil.verifyCode(user.getTotpSecret(), code)) {
            user.setTotpEnabled(true);
            userService.save(user);
            auditService.logAsync(user.getId(), "2FA_ENABLED", "User", user.getId(), "Two-factor authentication enabled");
            redirectAttributes.addFlashAttribute("success", "Two-factor authentication enabled successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid code. Please try again.");
            return "redirect:/profile/setup-2fa";
        }
        return "redirect:/profile";
    }

    @PostMapping("/remove-photo")
    public String removePhoto(Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String userId = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
        userService.findById(userId).ifPresent(user -> {
            user.setProfilePhoto(null);
            userService.save(user);
        });
        redirectAttributes.addFlashAttribute("success", "Profile photo removed");
        return "redirect:/profile";
    }

    @PostMapping("/disable-2fa")
    public String disable2fa(Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        userService.save(user);
        auditService.logAsync(user.getId(), "2FA_DISABLED", "User", user.getId(), "Two-factor authentication disabled");
        redirectAttributes.addFlashAttribute("success", "Two-factor authentication disabled.");
        return "redirect:/profile";
    }
}

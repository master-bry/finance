package com.master.finance.controller;

import com.master.finance.model.User;
import com.master.finance.service.AuditService;
import com.master.finance.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/password")
public class PasswordController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/change")
    public String showChangePasswordForm(@RequestParam(value = "forced", required = false) boolean forced,
                                         Authentication authentication, Model model) {
        model.addAttribute("forced", forced);
        model.addAttribute("currentPage", "password");
        model.addAttribute("pageSubtitle", forced ? "Your password has expired. Please set a new one." : "Change your password");
        model.addAttribute("title", forced ? "Password Expired - Change Required" : "Change Password");
        return "password/change";
    }

    @PostMapping("/change")
    public String changePassword(Authentication authentication,
                                 @RequestParam(required = false) String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 @RequestParam(value = "forced", required = false) boolean forced,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        String userId = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();

        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!forced) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
                return "redirect:/password/change";
            }
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match");
            redirectAttributes.addFlashAttribute("forced", forced);
            return "redirect:/password/change" + (forced ? "?forced=true" : "");
        }

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters");
            redirectAttributes.addFlashAttribute("forced", forced);
            return "redirect:/password/change" + (forced ? "?forced=true" : "");
        }

        if (forced && passwordEncoder.matches(newPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "New password must be different from the current password");
            redirectAttributes.addFlashAttribute("forced", true);
            return "redirect:/password/change?forced=true";
        }

        userService.changePassword(userId, newPassword);
        auditService.logAsync(userId, "PASSWORD_CHANGE", "User", userId, "Password changed" + (forced ? " (forced)" : ""));

        if (forced) {
            redirectAttributes.addFlashAttribute("success", "Password changed successfully. Your password has been reset.");
            return "redirect:/dashboard";
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        redirectAttributes.addFlashAttribute("success", "Password changed. Please login with your new password.");
        return "redirect:/login";
    }
}

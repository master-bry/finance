package com.master.finance.config;

import com.master.finance.model.User;
import com.master.finance.service.CurrencyService;
import com.master.finance.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private static final String USER_ATTR = "GlobalControllerAdvice.currentUser";

    @Autowired
    private UserService userService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private HttpServletRequest request;

    private User getCurrentUser(Authentication authentication) {
        User cached = (User) request.getAttribute(USER_ATTR);
        if (cached != null) return cached;
        if (authentication == null || !authentication.isAuthenticated()) return null;
        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null) request.setAttribute(USER_ATTR, user);
        return user;
    }

    @ModelAttribute("currentUser")
    public User addCurrentUser(Authentication authentication) {
        return getCurrentUser(authentication);
    }

    @ModelAttribute("userCurrency")
    public String addUserCurrency(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return user != null ? user.getCurrency() : "TZS";
    }

    @ModelAttribute("conversionRate")
    public Double addConversionRate(Authentication authentication) {
        User user = getCurrentUser(authentication);
        String currency = user != null ? user.getCurrency() : "TZS";
        return currencyService.getExchangeRate(currency);
    }

    @ModelAttribute("passwordExpired")
    public boolean addPasswordExpired(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return user != null && user.getPasswordChangedAt() != null
            && user.getPasswordChangedAt().plusDays(90).isBefore(java.time.LocalDateTime.now());
    }

    @ModelAttribute("passwordDaysRemaining")
    public long addPasswordDaysRemaining(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null || user.getPasswordChangedAt() == null) return 90;
        return java.time.LocalDateTime.now().until(
            user.getPasswordChangedAt().plusDays(90),
            java.time.temporal.ChronoUnit.DAYS
        );
    }
}

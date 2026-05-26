package com.master.finance.config;

import com.master.finance.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");

        if (username != null) {
            loginAttemptService.loginFailed(username);

            if (loginAttemptService.isBlocked(username)) {
                String errorMsg = "Account temporarily locked due to too many failed attempts. Try again in 15 minutes.";
                super.setDefaultFailureUrl("/login?error=" + java.net.URLEncoder.encode(errorMsg, "UTF-8"));
            } else {
                int remaining = loginAttemptService.getRemainingAttempts(username);
                String errorMsg = "Invalid username or password. " + remaining + " attempt(s) remaining.";
                super.setDefaultFailureUrl("/login?error=" + java.net.URLEncoder.encode(errorMsg, "UTF-8"));
            }
        }

        super.onAuthenticationFailure(request, response, exception);
    }
}

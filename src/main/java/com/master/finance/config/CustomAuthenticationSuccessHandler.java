package com.master.finance.config;

import com.master.finance.model.User;
import com.master.finance.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        User user = userService.findByUsername(username).orElse(null);

        if (user != null && userService.isPasswordExpired(user)) {
            getRedirectStrategy().sendRedirect(request, response, "/password/change?forced=true");
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, "/dashboard");
    }
}

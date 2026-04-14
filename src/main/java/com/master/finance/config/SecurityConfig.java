package com.master.finance.config;

import com.master.finance.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // Public resources and pages
                .requestMatchers(
                    "/", "/login", "/register",
                    "/auth/logout",               // custom logout page
                    "/error/**",                  // custom error pages
                    "/css/**", "/js/**", "/static/**",
                    "/images/**", "/debug/**", "/reset/**", "/fix-password",
                    "/test-login/**", "/check-user", "/set-password/**",
                    "/reencode-password", "/generate-hash", "/update-my-password"
                ).permitAll()
                // All authenticated pages (dashboard, transactions, etc.)
                .requestMatchers(
                    "/dashboard", "/transactions/**", "/debts/**",
                    "/investments/**", "/goals/**", "/budget/**",
                    "/excel/**", "/reports/**"
                ).authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/auth/logout")   // ← use custom logout page
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
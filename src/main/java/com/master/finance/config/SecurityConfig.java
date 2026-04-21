package com.master.finance.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.master.finance.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // Added for method-level security (optional but good)
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
                    "/auth/logout",               // custom logout page (if exists)
                    "/error/**",
                    "/css/**", "/js/**", "/static/**",
                    "/images/**", "/debug/**", "/reset/**", "/fix-password",
                    "/test-login/**", "/check-user", "/set-password/**",
                    "/reencode-password", "/generate-hash", "/update-my-password"
                ).permitAll()
                // All authenticated pages
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
                .logoutSuccessUrl("/login?logout")   // FIXED: use standard redirect instead of /auth/logout
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .userDetailsService(userDetailsService);

        // Optional: if you need H2 console, uncomment next line
        // http.headers().frameOptions().disable();

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
package com.master.finance.config;

import com.master.finance.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;

    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                    "/", "/login", "/register",
                    "/auth/logout",
                    "/error/**",
                    "/css/**", "/js/**", "/static/**",
                    "/images/**",
                    "/barcode/**",
                    "/verify/**"
                ).permitAll()
                .requestMatchers(
                    "/dashboard", "/transactions/**", "/debts/**",
                    "/investments/**", "/goals/**", "/budget/**",
                    "/excel/**", "/reports/**", "/profile/**"
                ).authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler)
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("FinanceTrackerSecureKey_2024!@#$")
                .tokenValiditySeconds(1209600)
                .userDetailsService(userDetailsService)
                .rememberMeParameter("remember-me")
                .authenticationSuccessHandler((request, response, authentication) -> {
                    response.sendRedirect("/dashboard");
                })
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .headers(headers -> headers
                .contentTypeOptions(options -> options.disable())
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )
            .userDetailsService(userDetailsService);

        return http.build();
    }

}

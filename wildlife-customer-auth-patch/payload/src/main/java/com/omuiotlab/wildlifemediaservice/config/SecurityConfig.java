package com.omuiotlab.wildlifemediaservice.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login.html",
                                "/login.js",
                                "/styles.css",
                                "/favicon.ico",
                                "/error",
                                "/actuator/health",
                                "/api/auth/csrf"
                        ).permitAll()
                        .requestMatchers("/admin.html", "/admin.js", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/cameras/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/cameras/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/cameras/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/media/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/media/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login.html?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login.html?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            } else {
                                response.sendRedirect("/login.html");
                            }
                        })
                );

        return http.build();
    }
}

package com.codearena.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/actuator/health"
                        ).permitAll()
                        // Public read endpoints
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/users/{username}",
                                "/api/v1/users/{username}/submissions",
                                "/api/v1/users/ratings",
                                "/api/v1/problems/**",
                                "/api/v1/contests/**",
                                "/api/v1/submissions",
                                "/api/v1/blogs/**"
                        ).permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                // Return a proper 401 for missing/invalid credentials and 403 for
                // authenticated-but-forbidden, both in the standard ApiResponse envelope
                // (Spring's default is 403 for both).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeError(res, HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED", "Authentication required"))
                        .accessDeniedHandler((req, res, e) ->
                                writeError(res, HttpServletResponse.SC_FORBIDDEN,
                                        "ACCESS_DENIED", "Access denied"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeError(HttpServletResponse res, int status, String code, String message)
            throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write(
                "{\"success\":false,\"errorCode\":\"" + code + "\",\"error\":\"" + message + "\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

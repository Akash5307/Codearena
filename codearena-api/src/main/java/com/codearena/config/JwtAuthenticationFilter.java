package com.codearena.config;

import com.codearena.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtil.isValidToken(token) && "access".equals(jwtUtil.getTokenType(token))) {
                // Check if token is blacklisted (logged out)
                Boolean isBlacklisted = redisTemplate.hasKey("blacklist:" + token);
                if (Boolean.TRUE.equals(isBlacklisted)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(userId, username, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}

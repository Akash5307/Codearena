package com.codearena.common.util;

import com.codearena.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final JwtConfig jwtConfig;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, "access", jwtConfig.getAccessExpiryMs());
    }

    public String generateRefreshToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, "refresh", jwtConfig.getRefreshExpiryMs());
    }

    private String buildToken(Long userId, String username, String role, String type, long expiryMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValidToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String getTokenType(String token) {
        return parseToken(token).get("type", String.class);
    }

    public long getAccessExpiryMs() {
        return jwtConfig.getAccessExpiryMs();
    }
}

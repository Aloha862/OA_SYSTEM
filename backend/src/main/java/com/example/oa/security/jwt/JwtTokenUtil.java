package com.example.oa.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    private final JwtProperties jwtProperties;

    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration());
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            return !claims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Claims claims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String username(String token) {
        return claims(token).getSubject();
    }

    public Long userId(String token) {
        Object userId = claims(token).get("userId");
        return userId == null ? null : Long.valueOf(String.valueOf(userId));
    }

    public String role(String token) {
        Object role = claims(token).get("role");
        return role == null ? null : String.valueOf(role);
    }

    public long expirationMillis() {
        return jwtProperties.getExpiration();
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}

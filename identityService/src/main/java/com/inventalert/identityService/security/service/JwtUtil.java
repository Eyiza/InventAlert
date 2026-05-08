package com.inventalert.identityService.security.service;

import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${JWT_SECRET}")
    private String secret;

    private static final long EXPIRY_MS = 24 * 60 * 60 * 1000L; // 24 hours

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("companyId", user.getCompanyId());
        claims.put("role", user.getRole().name());
        if (user.getRole() == Role.WAREHOUSE_STAFF && user.getWarehouseId() != null) {
            claims.put("warehouseId", user.getWarehouseId());
        }
        return buildToken(claims, user.getId());
    }

    public String generateSuperAdminToken(String superAdminId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "SUPER_ADMIN");
        // No companyId — SuperAdmin is platform-level, not scoped to any company
        return buildToken(claims, superAdminId);
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractCompanyId(String token) {
        return extractClaims(token).get("companyId", String.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public String extractWarehouseId(String token) {
        return extractClaims(token).get("warehouseId", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

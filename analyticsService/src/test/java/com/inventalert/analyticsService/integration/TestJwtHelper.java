package com.inventalert.analyticsService.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestJwtHelper {

    static final String TEST_SECRET = "test-secret-key-minimum-32-chars!!";
    private static final long EXPIRY_MS = 24 * 60 * 60 * 1000L;

    static String buildToken(String userId, String companyId, String role) {
        Map<String, Object> claims = new HashMap<>();
        if (companyId != null) claims.put("companyId", companyId);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(signingKey())
                .compact();
    }

    static String buildAdminToken(String companyId) {
        return buildToken("user-admin", companyId, "ADMIN");
    }

    static String buildManagerToken(String companyId) {
        return buildToken("user-manager", companyId, "MANAGER");
    }

    static String buildSuperAdminToken() {
        return buildToken("super-admin", null, "SUPER_ADMIN");
    }

    static String buildWarehouseStaffToken(String companyId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("companyId", companyId);
        claims.put("role", "WAREHOUSE_STAFF");
        claims.put("warehouseId", "wh-test");
        return Jwts.builder()
                .claims(claims)
                .subject("user-staff")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(signingKey())
                .compact();
    }

    private static SecretKey signingKey() {
        return Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }
}

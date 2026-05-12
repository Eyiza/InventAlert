package com.inventalert.analyticsService.security;

import com.inventalert.analyticsService.security.service.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "test-secret-key-minimum-32-chars!!";
    private static final long EXPIRY_MS = 24 * 60 * 60 * 1000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
    }

    @Test
    void extractUserId_returnsSubjectClaim() {
        String token = buildToken("user-123", Map.of("companyId", "co-1", "role", "ADMIN"));
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-123");
    }

    @Test
    void extractCompanyId_returnsCompanyIdClaim() {
        String token = buildToken("u", Map.of("companyId", "co-abc", "role", "MANAGER"));
        assertThat(jwtUtil.extractCompanyId(token)).isEqualTo("co-abc");
    }

    @Test
    void extractRole_returnsRoleClaim() {
        String token = buildToken("u", Map.of("role", "WAREHOUSE_STAFF"));
        assertThat(jwtUtil.extractRole(token)).isEqualTo("WAREHOUSE_STAFF");
    }

    @Test
    void extractWarehouseId_presentClaim_returnsValue() {
        String token = buildToken("u", Map.of("role", "WAREHOUSE_STAFF", "warehouseId", "wh-99"));
        assertThat(jwtUtil.extractWarehouseId(token)).isEqualTo("wh-99");
    }

    @Test
    void extractWarehouseId_absentClaim_returnsNull() {
        String token = buildToken("u", Map.of("role", "ADMIN", "companyId", "co-1"));
        assertThat(jwtUtil.extractWarehouseId(token)).isNull();
    }

    @Test
    void extractCompanyId_superAdminToken_returnsNull() {
        String token = buildToken("super-admin", Map.of("role", "SUPER_ADMIN"));
        assertThat(jwtUtil.extractCompanyId(token)).isNull();
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = buildToken("u", Map.of("role", "ADMIN"));
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        String expired = Jwts.builder()
                .claims(Map.of("role", "ADMIN"))
                .subject("u")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(signingKey())
                .compact();
        assertThat(jwtUtil.isTokenValid(expired)).isFalse();
    }

    @Test
    void isTokenValid_tamperedSignature_returnsFalse() {
        String token = buildToken("u", Map.of("role", "ADMIN"));
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_emptyString_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("")).isFalse();
    }

    @Test
    void isTokenValid_randomString_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("not.a.token")).isFalse();
    }

    private String buildToken(String subject, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(signingKey())
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }
}

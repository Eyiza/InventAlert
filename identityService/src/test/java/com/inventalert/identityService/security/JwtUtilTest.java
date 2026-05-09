package com.inventalert.identityService.security;

import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import com.inventalert.identityService.security.service.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "test-secret-key-minimum-32-chars!!";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
    }

    @Test
    void generateToken_containsCorrectClaims() {
        User user = buildUser("user-1", "company-1", Role.ADMIN, null);

        String token = jwtUtil.generateToken(user, null);

        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-1");
        assertThat(jwtUtil.extractCompanyId(token)).isEqualTo("company-1");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.extractWarehouseId(token)).isNull();
    }

    @Test
    void generateToken_warehouseStaff_containsWarehouseId() {
        User user = buildUser("user-2", "company-1", Role.WAREHOUSE_STAFF, "wh-abc");

        String token = jwtUtil.generateToken(user, "wh-abc");

        assertThat(jwtUtil.extractWarehouseId(token)).isEqualTo("wh-abc");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("WAREHOUSE_STAFF");
    }

    @Test
    void generateToken_nonStaff_noWarehouseId() {
        User user = buildUser("user-3", "company-1", Role.MANAGER, "wh-should-be-ignored");

        String token = jwtUtil.generateToken(user, null);

        assertThat(jwtUtil.extractWarehouseId(token)).isNull();
    }

    @Test
    void generateToken_warehouseStaff_nullWarehouseId_noWarehouseIdClaim() {
        User user = buildUser("user-4", "company-1", Role.WAREHOUSE_STAFF, null);

        String token = jwtUtil.generateToken(user, null);

        assertThat(jwtUtil.extractWarehouseId(token)).isNull();
    }

    @Test
    void generateSuperAdminToken_hasNoCompanyId() {
        String token = jwtUtil.generateSuperAdminToken("super-admin-id");

        assertThat(jwtUtil.extractUserId(token)).isEqualTo("super-admin-id");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SUPER_ADMIN");
        assertThat(jwtUtil.extractCompanyId(token)).isNull();
        assertThat(jwtUtil.extractWarehouseId(token)).isNull();
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtUtil.generateSuperAdminToken("any-id");

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_tamperedSignature_returnsFalse() {
        String token = jwtUtil.generateSuperAdminToken("any-id");
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        String expiredToken = Jwts.builder()
                .setSubject("user-x")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000))
                .setExpiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtUtil.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void isTokenValid_emptyString_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("")).isFalse();
    }

    @Test
    void extractUserId_returnsSubClaim() {
        User user = buildUser("user-abc", "co-1", Role.PROCUREMENT_OFFICER, null);
        String token = jwtUtil.generateToken(user, null);

        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-abc");
    }

    @Test
    void extractRole_returnsRoleString() {
        User user = buildUser("u", "c", Role.MANAGER, null);
        String token = jwtUtil.generateToken(user, null);

        assertThat(jwtUtil.extractRole(token)).isEqualTo("MANAGER");
    }

    private User buildUser(String id, String companyId, Role role, String warehouseId) {
        return User.builder()
                .id(id)
                .companyId(companyId)
                .email("tunde@test.ng")
                .passwordHash("hash")
                .role(role)
                .build();
    }
}

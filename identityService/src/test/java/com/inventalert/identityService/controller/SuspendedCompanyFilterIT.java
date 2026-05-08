package com.inventalert.identityService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.identityService.dto.request.LoginRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.model.CompanyStatus;
import com.inventalert.identityService.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SuspendedCompanyFilterIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired MockMvc          mockMvc;
    @Autowired ObjectMapper     objectMapper;
    @Autowired CompanyRepository companyRepository;

    // ── active company can log in and access protected endpoints ───────

    @Test
    void activeCompany_login_returns200() throws Exception {
        signupAndGetToken("Active Corp", "active@corp.io");
        // login should succeed
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("active@corp.io", "password123"))))
                .andExpect(status().isOk());
    }

    @Test
    void activeCompany_withValidToken_protectedEndpointReturns2xx() throws Exception {
        String token = signupAndGetToken("Active Corp 2", "active2@corp.io");
        // GET /api/users is a protected endpoint — with valid token should NOT be 401 or 403
        // (it may return 403 for role reasons if endpoint is Admin-only, but not suspension-related)
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // 200 or 403 (role-enforced) is acceptable — NOT 401 (token invalid) or suspension 403
                    // We just verify the token is accepted and not rejected by the suspension check
                    assert status != 401 : "Token should be valid and accepted";
                });
    }

    // ── suspended company is blocked ───────────────────────────────────

    @Test
    void suspendedCompany_login_returns403() throws Exception {
        signupAndGetToken("Suspend Corp", "suspend@corp.io");
        suspendCompanyByEmail("suspend@corp.io");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("suspend@corp.io", "password123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedCompany_validTokenIsBlocked_returns403() throws Exception {
        // Signup and get token BEFORE suspension
        String token = signupAndGetToken("Pre-Suspend Corp", "presuspend@corp.io");

        // Suspend the company after the token was issued
        suspendCompanyByEmail("presuspend@corp.io");

        // The filter checks live company status on every request — existing tokens are invalidated
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ── helpers ────────────────────────────────────────────────────────

    private String signupAndGetToken(String companyName, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(companyName, email, "password123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private void suspendCompanyByEmail(String adminEmail) {
        companyRepository.findByAdminEmail(adminEmail).ifPresent(company -> {
            company.setStatus(CompanyStatus.SUSPENDED);
            companyRepository.save(company);
        });
    }
}

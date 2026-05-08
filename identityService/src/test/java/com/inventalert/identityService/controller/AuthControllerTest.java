package com.inventalert.identityService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.identityService.dto.request.LoginRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.model.CompanyStatus;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.security.service.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired MockMvc           mockMvc;
    @Autowired ObjectMapper      objectMapper;
    @Autowired JwtUtil           jwtUtil;
    @Autowired CompanyRepository companyRepository;

    // ── Signup ────────────────────────────────────────────────────────────────

    @Test
    void signup_validRequest_returns201AndToken() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setCompanyName("Eko Atlantic Inc");
        req.setAdminEmail("admin@ekoatlantic.ng");
        req.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.companyId").isNotEmpty())
                .andExpect(jsonPath("$.warehouseId").doesNotExist())
                .andReturn();

        String body  = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.extractCompanyId(token)).isNotNull();
        assertThat(jwtUtil.extractUserId(token)).isNotNull();
    }

    @Test
    void signup_duplicateEmail_returns409() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setCompanyName("Konga Ltd");
        req.setAdminEmail("info@konga.ng");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void signup_missingPassword_returns400() throws Exception {
        String body = """
                {"companyName":"Corp","adminEmail":"a@b.ng"}
                """;
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_invalidEmail_returns400() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setCompanyName("Corp");
        req.setAdminEmail("not-an-email");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_correctCredentials_returns200AndToken() throws Exception {
        SignupRequest signupReq = new SignupRequest();
        signupReq.setCompanyName("Paystack HQ");
        signupReq.setAdminEmail("login@paystack.ng");
        signupReq.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("login@paystack.ng");
        loginReq.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        SignupRequest signupReq = new SignupRequest();
        signupReq.setCompanyName("Interswitch Ltd");
        signupReq.setAdminEmail("ops@interswitch.ng");
        signupReq.setPassword("password123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("ops@interswitch.ng");
        loginReq.setPassword("WRONGPASSWORD");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@nowhere.ng");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingEmail_returns400() throws Exception {
        String body = """
                {"password":"password123"}
                """;
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── Super-admin login ─────────────────────────────────────────────────────

    @Test
    void superAdminLogin_correctCredentials_returns200NoCompanyId() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("superadmin@inventalert.ng");
        req.setPassword("SuperSecure123!");

        MvcResult result = mockMvc.perform(post("/api/auth/superadmin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        assertThat(jwtUtil.extractCompanyId(token)).isNull();
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void superAdminLogin_wrongPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("superadmin@inventalert.ng");
        req.setPassword("WrongPass!");

        mockMvc.perform(post("/api/auth/superadmin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ── Suspended company filter ──────────────────────────────────────────────

    @Test
    void activeCompany_login_returns200() throws Exception {
        signupAndGetToken("Zenith Corp", "active@zenith.ng");

        LoginRequest req = new LoginRequest();
        req.setEmail("active@zenith.ng");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void activeCompany_withValidToken_protectedEndpointReturns2xx() throws Exception {
        String token = signupAndGetToken("GTB Holdings", "holdings@gtb.ng");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 : "Token should be valid and accepted";
                });
    }

    @Test
    void suspendedCompany_login_returns403() throws Exception {
        signupAndGetToken("Suspended Foods Ltd", "admin@susfoods.ng");
        suspendCompanyByEmail("admin@susfoods.ng");

        LoginRequest req = new LoginRequest();
        req.setEmail("admin@susfoods.ng");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedCompany_validTokenIsBlocked_returns403() throws Exception {
        String token = signupAndGetToken("PreSuspend Energy", "ops@presuspend.ng");
        suspendCompanyByEmail("ops@presuspend.ng");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String signupAndGetToken(String companyName, String email) throws Exception {
        SignupRequest req = new SignupRequest();
        req.setCompanyName(companyName);
        req.setAdminEmail(email);
        req.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
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

package com.inventalert.identityService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.identityService.dto.request.LoginRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.security.service.JwtUtil;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtUtil      jwtUtil;

    // ── POST /api/auth/signup ──────────────────────────────────────────

    @Test
    void signup_validRequest_returns201AndToken() throws Exception {
        SignupRequest req = new SignupRequest("Test Corp", "admin@testcorp.io", "password123");

        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.companyId").isNotEmpty())
                .andExpect(jsonPath("$.warehouseId").doesNotExist())
                .andReturn();

        // Verify JWT shape — must contain companyId, userId, role=ADMIN
        String body   = result.getResponse().getContentAsString();
        String token  = objectMapper.readTree(body).get("token").asText();
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.extractCompanyId(token)).isNotNull();
        assertThat(jwtUtil.extractUserId(token)).isNotNull();
    }

    @Test
    void signup_duplicateEmail_returns409() throws Exception {
        SignupRequest req = new SignupRequest("Dupe Corp", "dup@corp.io", "password123");
        // First signup
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
        // Second signup same email
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void signup_missingPassword_returns400() throws Exception {
        String body = """
                {"companyName":"Corp","adminEmail":"a@b.io"}
                """;
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_invalidEmail_returns400() throws Exception {
        SignupRequest req = new SignupRequest("Corp", "not-an-email", "password123");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/login ───────────────────────────────────────────

    @Test
    void login_correctCredentials_returns200AndToken() throws Exception {
        // Signup first to create a user
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest("Login Corp", "login@corp.io", "password123"))))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("login@corp.io", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest("Wrong Pass Corp", "wrongpass@corp.io", "password123"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("wrongpass@corp.io", "WRONGPASSWORD"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("nobody@nowhere.io", "password123"))))
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

    // ── POST /api/auth/superadmin/login ────────────────────────────────

    @Test
    void superAdminLogin_correctCredentials_returns200NoCompanyId() throws Exception {
        LoginRequest req = new LoginRequest("superadmin@inventalert.io", "SuperSecure123!");

        MvcResult result = mockMvc.perform(post("/api/auth/superadmin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
                .andReturn();

        // Verify JWT has no companyId claim
        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        assertThat(jwtUtil.extractCompanyId(token)).isNull();
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void superAdminLogin_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/superadmin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("superadmin@inventalert.io", "WrongPass!"))))
                .andExpect(status().isUnauthorized());
    }
}

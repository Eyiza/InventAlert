package com.inventalert.identityService.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.identityService.dto.request.LoginRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.repository.WarehouseAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SuperAdminControllerIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository                userRepository;
    @Autowired CompanyRepository             companyRepository;
    @Autowired WarehouseAssignmentRepository assignmentRepository;

    private String superAdminToken;

    @BeforeEach
    void setup() throws Exception {
        assignmentRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();
        superAdminToken = fetchSuperAdminToken();
    }

    // ── List all companies ────────────────────────────────────────────────────

    @Test
    void listAllCompanies_returns200WithAllCompanies() throws Exception {
        signup("Alpha Corp", "admin@alpha.com");
        signup("Beta Corp",  "admin@beta.com");

        mockMvc.perform(get("/api/superadmin/companies")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listAllCompanies_adminToken_returns403() throws Exception {
        String adminToken = signup("Gamma Corp", "admin@gamma.com");

        mockMvc.perform(get("/api/superadmin/companies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAllCompanies_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/superadmin/companies"))
                .andExpect(status().isUnauthorized());
    }

    // ── Suspend company ───────────────────────────────────────────────────────

    @Test
    void suspendCompany_returns200WithStatusSuspended() throws Exception {
        String companyId = signupAndGetCompanyId("Delta Corp", "admin@delta.com");

        mockMvc.perform(patch("/api/superadmin/companies/" + companyId + "/suspend")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void suspendCompany_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/superadmin/companies/nonexistent-id/suspend")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void suspendCompany_adminToken_returns403() throws Exception {
        String adminToken  = signup("Epsilon Corp", "admin@epsilon.com");
        String companyId   = companyRepository.findByAdminEmail("admin@epsilon.com")
                .orElseThrow().getId();

        mockMvc.perform(patch("/api/superadmin/companies/" + companyId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    // ── Reactivate company ────────────────────────────────────────────────────

    @Test
    void reactivateCompany_returns200WithStatusActive() throws Exception {
        String companyId = signupAndGetCompanyId("Zeta Corp", "admin@zeta.com");

        mockMvc.perform(patch("/api/superadmin/companies/" + companyId + "/suspend")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/superadmin/companies/" + companyId + "/reactivate")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void reactivateCompany_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/superadmin/companies/nonexistent-id/reactivate")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNotFound());
    }

    // ── Company offboarding ───────────────────────────────────────────────────

    @Test
    void offboardCompany_adminToken_returns200() throws Exception {
        String adminToken = signup("Eta Corp", "admin@eta.com");

        mockMvc.perform(delete("/api/companies/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void offboardCompany_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/companies/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String signup(String companyName, String email) throws Exception {
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

    private String signupAndGetCompanyId(String companyName, String email) throws Exception {
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
                .get("companyId").asText();
    }

    private String fetchSuperAdminToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("superadmin@inventalert.ng");
        req.setPassword("SuperSecure123!");

        MvcResult result = mockMvc.perform(post("/api/auth/superadmin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }
}

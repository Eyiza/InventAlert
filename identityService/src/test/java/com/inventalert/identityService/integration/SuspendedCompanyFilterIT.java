package com.inventalert.identityService.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.identityService.dto.request.CreateUserRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.model.CompanyStatus;
import com.inventalert.identityService.model.Role;
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
class SuspendedCompanyFilterIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CompanyRepository             companyRepository;
    @Autowired UserRepository                userRepository;
    @Autowired WarehouseAssignmentRepository assignmentRepository;

    @BeforeEach
    void clean() {
        assignmentRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    void activeCompany_allUserEndpointsAccessible() throws Exception {
        String token = signupAndGetToken("Active Corp", "admin@active.com");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("staff@active.com", "password123", Role.MANAGER))))
                .andExpect(status().isCreated());
    }

    @Test
    void suspendedCompany_getUsers_returns403() throws Exception {
        String token = signupAndGetToken("Frozen Corp", "admin@frozen.com");
        suspendByEmail("admin@frozen.com");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedCompany_createUser_returns403() throws Exception {
        String token = signupAndGetToken("Ice Corp", "admin@ice.com");
        suspendByEmail("admin@ice.com");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("staff@ice.com", "password123", Role.MANAGER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedCompany_updateRole_returns403() throws Exception {
        String token = signupAndGetToken("Snow Corp", "admin@snow.com");

        // Create a user while company is still active
        String userId = createUserAndGetId(token, "staff@snow.com", Role.MANAGER);

        suspendByEmail("admin@snow.com");

        mockMvc.perform(patch("/api/users/" + userId + "/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"PROCUREMENT_OFFICER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedCompany_deactivateUser_returns403() throws Exception {
        String token = signupAndGetToken("Hail Corp", "admin@hail.com");
        String userId = createUserAndGetId(token, "staff@hail.com", Role.MANAGER);
        suspendByEmail("admin@hail.com");

        mockMvc.perform(patch("/api/users/" + userId + "/deactivate")
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

    private String createUserAndGetId(String adminToken, String email, Role role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest(email, "password123", role))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private void suspendByEmail(String adminEmail) {
        companyRepository.findByAdminEmail(adminEmail).ifPresent(c -> {
            c.setStatus(CompanyStatus.SUSPENDED);
            companyRepository.save(c);
        });
    }
}

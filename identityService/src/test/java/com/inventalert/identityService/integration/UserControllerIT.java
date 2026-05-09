package com.inventalert.identityService.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.identityService.dto.request.AssignWarehouseRequest;
import com.inventalert.identityService.dto.request.CreateUserRequest;
import com.inventalert.identityService.dto.request.LoginRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.dto.request.UpdateRoleRequest;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserControllerIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired MockMvc      mockMvc;
    private final ObjectMapper objectMapper;
    @Autowired UserRepository                userRepository;
    @Autowired CompanyRepository             companyRepository;
    @Autowired WarehouseAssignmentRepository assignmentRepository;

    @Autowired
    UserControllerIT(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void clean() {
        assignmentRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();
    }

    // ── Create user ───────────────────────────────────────────────────────────

    @Test
    void createUser_success_returns201() throws Exception {
        String token = signupAndGetToken("Acme Corp", "admin@acme.com");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("staff@acme.com", "password123", Role.MANAGER, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("staff@acme.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void createUser_duplicateEmail_returns409() throws Exception {
        String token = signupAndGetToken("Beta Corp", "admin@beta.com");
        CreateUserRequest req = new CreateUserRequest("dup@beta.com", "password123", Role.MANAGER, null);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_missingRole_returns400() throws Exception {
        String token = signupAndGetToken("Gamma Corp", "admin@gamma.com");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@gamma.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("x@test.com", "password123", Role.MANAGER, null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser_managerToken_returns403() throws Exception {
        String adminToken = signupAndGetToken("Delta Corp", "admin@delta.com");

        // Admin creates a manager
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("mgr@delta.com", "password123", Role.MANAGER, null))))
                .andExpect(status().isCreated());

        // Log in as manager
        String managerToken = loginAndGetToken("mgr@delta.com", "password123");

        // Manager tries to create a user — should be forbidden
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("new@delta.com", "password123", Role.MANAGER, null))))
                .andExpect(status().isForbidden());
    }

    // ── List users ────────────────────────────────────────────────────────────

    @Test
    void listUsers_returns200WithCorrectCount() throws Exception {
        String token = signupAndGetToken("Epsilon Corp", "admin@epsilon.com");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("u1@epsilon.com", "password123", Role.MANAGER, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest("u2@epsilon.com", "password123", Role.PROCUREMENT_OFFICER, null))))
                .andExpect(status().isCreated());

        // 3 total: the admin from signup + 2 created here
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // ── Update role ───────────────────────────────────────────────────────────

    @Test
    void updateRole_returns200WithNewRole() throws Exception {
        String token = signupAndGetToken("Zeta Corp", "admin@zeta.com");
        String userId = createUserAndGetId(token, "staff@zeta.com", Role.MANAGER);

        mockMvc.perform(patch("/api/users/" + userId + "/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleRequest(Role.PROCUREMENT_OFFICER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROCUREMENT_OFFICER"));
    }

    @Test
    void updateRole_unknownUser_returns404() throws Exception {
        String token = signupAndGetToken("Eta Corp", "admin@eta.com");

        mockMvc.perform(patch("/api/users/nonexistent-id/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleRequest(Role.MANAGER))))
                .andExpect(status().isNotFound());
    }

    // ── Deactivate user ───────────────────────────────────────────────────────

    @Test
    void deactivateUser_returns200WithIsActiveFalse() throws Exception {
        String token = signupAndGetToken("Theta Corp", "admin@theta.com");
        String userId = createUserAndGetId(token, "staff@theta.com", Role.MANAGER);

        mockMvc.perform(patch("/api/users/" + userId + "/deactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void deactivateUser_alreadyInactive_returns409() throws Exception {
        String token = signupAndGetToken("Iota Corp", "admin@iota.com");
        String userId = createUserAndGetId(token, "staff@iota.com", Role.MANAGER);

        mockMvc.perform(patch("/api/users/" + userId + "/deactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/users/" + userId + "/deactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    // ── Warehouse assignment ──────────────────────────────────────────────────

    @Test
    void assignToWarehouse_returns201() throws Exception {
        String token = signupAndGetToken("Kappa Corp", "admin@kappa.com");
        String userId = createUserAndGetId(token, "staff@kappa.com", Role.WAREHOUSE_STAFF);

        mockMvc.perform(post("/api/users/" + userId + "/assign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignWarehouseRequest("wh-001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseId").value("wh-001"));
    }

    @Test
    void assignToWarehouse_duplicate_returnsExistingAssignment() throws Exception {
        String token = signupAndGetToken("Lambda Corp", "admin@lambda.com");
        String userId = createUserAndGetId(token, "staff@lambda.com", Role.WAREHOUSE_STAFF);

        mockMvc.perform(post("/api/users/" + userId + "/assign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignWarehouseRequest("wh-002"))))
                .andExpect(status().isCreated());

        // Second assign to same warehouse — still 201, no duplicate row
        mockMvc.perform(post("/api/users/" + userId + "/assign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignWarehouseRequest("wh-002"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseId").value("wh-002"));

        assertThat(assignmentRepository.findAllByUserId(userId)).hasSize(1);
    }

    @Test
    void getAssignments_returns200WithList() throws Exception {
        String token = signupAndGetToken("Mu Corp", "admin@mu.com");
        String userId = createUserAndGetId(token, "staff@mu.com", Role.WAREHOUSE_STAFF);

        mockMvc.perform(post("/api/users/" + userId + "/assign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignWarehouseRequest("wh-003"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/" + userId + "/assignments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].warehouseId").value("wh-003"));
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

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private String createUserAndGetId(String adminToken, String email, Role role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest(email, "password123", role, null))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}

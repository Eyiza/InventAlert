package com.inventalert.identityService.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.identityService.dto.request.ForgotPasswordRequest;
import com.inventalert.identityService.dto.request.LoginRequest;
import com.inventalert.identityService.dto.request.ResetPasswordRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.model.PasswordResetToken;
import com.inventalert.identityService.repository.PasswordResetTokenRepository;
import com.inventalert.identityService.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthPasswordResetIT {

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

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordResetTokenRepository tokenRepository;
    @Autowired UserRepository userRepository;

    // ── forgot-password ───────────────────────────────────────────────────────

    @Test
    void forgotPassword_knownEmail_returns200AndCreatesToken() throws Exception {
        signup("Dangote Cement", "admin@dangote.ng");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("admin@dangote.ng"))))
                .andExpect(status().isOk());

        String userId = userRepository.findByEmail("admin@dangote.ng").orElseThrow().getId();
        assertThat(tokenRepository.findByUserIdAndUsedFalse(userId)).isPresent();
    }

    @Test
    void forgotPassword_unknownEmail_stillReturns200_noTokenCreated() throws Exception {
        long countBefore = tokenRepository.count();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("ghost@nowhere.ng"))))
                .andExpect(status().isOk());

        assertThat(tokenRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void forgotPassword_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_secondRequest_invalidatesOldTokenAndIssuesNew() throws Exception {
        signup("MTN Nigeria", "ops@mtn.ng");

        // First request
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("ops@mtn.ng"))))
                .andExpect(status().isOk());

        String userId = userRepository.findByEmail("ops@mtn.ng").orElseThrow().getId();
        String firstToken = tokenRepository.findByUserIdAndUsedFalse(userId)
                .orElseThrow().getToken();

        // Second request
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("ops@mtn.ng"))))
                .andExpect(status().isOk());

        String secondToken = tokenRepository.findByUserIdAndUsedFalse(userId)
                .orElseThrow().getToken();

        assertThat(secondToken).isNotEqualTo(firstToken);
        // Old token must now be marked used
        assertThat(tokenRepository.findByToken(firstToken).orElseThrow().isUsed()).isTrue();
    }

    // ── reset-password ────────────────────────────────────────────────────────

    @Test
    void resetPassword_validToken_returns200AndPasswordIsChanged() throws Exception {
        signup("Flutterwave Ltd", "dev@flutterwave.ng");

        // Request reset
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("dev@flutterwave.ng"))))
                .andExpect(status().isOk());

        String userId = userRepository.findByEmail("dev@flutterwave.ng").orElseThrow().getId();
        String token = tokenRepository.findByUserIdAndUsedFalse(userId).orElseThrow().getToken();

        // Reset
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(token, "NewSecure99!"))))
                .andExpect(status().isOk());

        // Token must be consumed
        assertThat(tokenRepository.findByToken(token).orElseThrow().isUsed()).isTrue();

        // Old password no longer works
        LoginRequest oldLogin = new LoginRequest();
        oldLogin.setEmail("dev@flutterwave.ng");
        oldLogin.setPassword("password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized());

        // New password works and returns a valid JWT
        LoginRequest newLogin = new LoginRequest();
        newLogin.setEmail("dev@flutterwave.ng");
        newLogin.setPassword("NewSecure99!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void resetPassword_invalidToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("non-existent-token", "NewSecure99!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_expiredToken_returns400() throws Exception {
        signup("Kuda Bank", "support@kuda.ng");
        String userId = userRepository.findByEmail("support@kuda.ng").orElseThrow().getId();

        PasswordResetToken expired = new PasswordResetToken();
        expired.setUserId(userId);
        expired.setToken("expired-token-kuda");
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        expired.setUsed(false);
        tokenRepository.save(expired);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("expired-token-kuda", "NewSecure99!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_alreadyUsedToken_returns400() throws Exception {
        signup("Opay Digital", "info@opay.ng");
        String userId = userRepository.findByEmail("info@opay.ng").orElseThrow().getId();

        PasswordResetToken used = new PasswordResetToken();
        used.setUserId(userId);
        used.setToken("used-token-opay");
        used.setExpiresAt(LocalDateTime.now().plusHours(1));
        used.setUsed(true);
        tokenRepository.save(used);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("used-token-opay", "NewSecure99!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_tokenConsumedOnFirstUse_secondUseReturns400() throws Exception {
        signup("Paystack Inc", "hello@paystack.ng");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("hello@paystack.ng"))))
                .andExpect(status().isOk());

        String userId = userRepository.findByEmail("hello@paystack.ng").orElseThrow().getId();
        String token = tokenRepository.findByUserIdAndUsedFalse(userId).orElseThrow().getToken();

        // First use succeeds
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(token, "NewSecure99!"))))
                .andExpect(status().isOk());

        // Second use of the same token must fail
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(token, "AnotherPass99!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_shortNewPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("some-token", "short"))))
                .andExpect(status().isBadRequest());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void signup(String companyName, String email) throws Exception {
        SignupRequest req = new SignupRequest();
        req.setCompanyName(companyName);
        req.setAdminEmail(email);
        req.setPassword("password123");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}

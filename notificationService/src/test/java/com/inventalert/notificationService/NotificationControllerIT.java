package com.inventalert.notificationService;

import com.inventalert.notificationService.model.Notification;
import com.inventalert.notificationService.model.NotificationType;
import com.inventalert.notificationService.security.JwtUtil;
import com.inventalert.notificationService.service.EmailService;
import com.inventalert.notificationService.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = "spring.kafka.listener.auto-startup=false")
class NotificationControllerIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired MockMvc mockMvc;
    @Autowired NotificationService notificationService;
    @Autowired StringRedisTemplate redisTemplate;

    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean EmailService emailService;
    @MockitoBean SimpMessagingTemplate messagingTemplate;

    private static final String VALID_TOKEN = "valid.test.token";

    @BeforeEach
    void setUp() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn("adebayo-001");
        when(jwtUtil.extractCompanyId(VALID_TOKEN)).thenReturn("konga-001");
        when(jwtUtil.extractRole(VALID_TOKEN)).thenReturn("STAFF");
        when(jwtUtil.extractWarehouseId(VALID_TOKEN)).thenReturn(null);
    }

    @Test
    void GetNotifications_AfterCreate_CheckIfReturnsNotificationTest() throws Exception {
        notificationService.create("evt-it-001", "konga-001", "adebayo-001", "adebayo@konga.ng",
                NotificationType.RESTOCK_ALERT, "Low stock on Indomie noodles", "alert-001");

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("RESTOCK_ALERT"))
                .andExpect(jsonPath("$[0].message").value("Low stock on Indomie noodles"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void MarkAsRead_ExistingNotification_CheckIfReturns200Test() throws Exception {
        Notification notification = notificationService.create(
                "evt-it-002", "konga-001", "adebayo-001", "adebayo@konga.ng",
                NotificationType.TRANSFER_SUGGESTION, "Transfer suggested for Dangote cement", "ts-001");

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getNotificationId())
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.notificationId").value(notification.getNotificationId()));
    }

    @Test
    void MarkAsRead_NotExisting_CheckIfReturns404Test() throws Exception {
        mockMvc.perform(patch("/api/notifications/ghost-id/read")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void GetUnreadCount_AfterCreate_CheckIfReturnsCorrectCountTest() throws Exception {
        notificationService.create("evt-it-003", "konga-001", "adebayo-001", "adebayo@konga.ng",
                NotificationType.RESTOCK_ALERT, "Low stock on Indomie noodles", "alert-003");
        notificationService.create("evt-it-004", "konga-001", "adebayo-001", "adebayo@konga.ng",
                NotificationType.TRANSFER_SUGGESTION, "Transfer suggested", "ts-004");

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void GetNotifications_NoToken_CheckIfReturns401Test() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }
}

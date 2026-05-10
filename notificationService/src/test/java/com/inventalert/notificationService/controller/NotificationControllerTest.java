package com.inventalert.notificationService.controller;

import com.inventalert.notificationService.dto.response.NotificationResponse;
import com.inventalert.notificationService.dto.response.UnreadCountResponse;
import com.inventalert.notificationService.exception.NotificationNotFoundException;
import com.inventalert.notificationService.model.NotificationType;
import com.inventalert.notificationService.security.JwtUtil;
import com.inventalert.notificationService.security.config.SecurityConfig;
import com.inventalert.notificationService.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean NotificationService notificationService;
    @MockitoBean JwtUtil jwtUtil;

    private static final String VALID_TOKEN = "valid.test.token";

    @BeforeEach
    void setUp() {
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn("adebayo-001");
        when(jwtUtil.extractCompanyId(VALID_TOKEN)).thenReturn("konga-001");
        when(jwtUtil.extractRole(VALID_TOKEN)).thenReturn("ADMIN");
        when(jwtUtil.extractWarehouseId(VALID_TOKEN)).thenReturn(null);
    }

    @Test
    void GetNotifications_WithValidToken_CheckIfReturns200Test() throws Exception {
        List<NotificationResponse> notifications = List.of(
                new NotificationResponse("notif-1", "konga-001", "adebayo-001",
                        NotificationType.RESTOCK_ALERT, "Low stock on Indomie noodles",
                        "alert-001", false, Instant.now())
        );
        when(notificationService.getNotifications("konga-001", "adebayo-001", 0, 20))
                .thenReturn(notifications);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].notificationId").value("notif-1"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void GetNotifications_NoToken_CheckIfReturns401Test() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GetNotifications_InvalidToken_CheckIfReturns401Test() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer this.is.not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void MarkAsRead_WithValidToken_CheckIfReturns200Test() throws Exception {
        NotificationResponse response = new NotificationResponse(
                "notif-1", "konga-001", "adebayo-001",
                NotificationType.RESTOCK_ALERT, "Low stock on Indomie noodles",
                "alert-001", true, Instant.now()
        );
        when(notificationService.markAsRead("konga-001", "notif-1")).thenReturn(response);

        mockMvc.perform(patch("/api/notifications/notif-1/read")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.notificationId").value("notif-1"));
    }

    @Test
    void MarkAsRead_NotFound_CheckIfReturns404Test() throws Exception {
        when(notificationService.markAsRead("konga-001", "ghost-id"))
                .thenThrow(new NotificationNotFoundException("ghost-id"));

        mockMvc.perform(patch("/api/notifications/ghost-id/read")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void GetUnreadCount_WithValidToken_CheckIfReturns200Test() throws Exception {
        when(notificationService.getUnreadCount("konga-001", "adebayo-001"))
                .thenReturn(new UnreadCountResponse(5));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }
}

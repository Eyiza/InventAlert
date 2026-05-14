package com.inventalert.notificationService.controller;

import com.inventalert.notificationService.dto.response.NotificationResponse;
import com.inventalert.notificationService.dto.response.UnreadCountResponse;
import com.inventalert.notificationService.security.model.JwtUser;
import com.inventalert.notificationService.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notifications", description = "REST polling for in-app notifications. Real-time delivery is via WebSocket (STOMP) at /ws — see websocket.md in docs/api/.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get notifications (paginated)",
               description = "Returns notifications for the authenticated user, newest first. Notifications are stored in Redis with a configurable TTL (default 90 days).")
    @ApiResponse(responseCode = "200", description = "Notification list")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal JwtUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                notificationService.getNotifications(principal.getCompanyId(), principal.getUserId(), page, size));
    }

    @Operation(summary = "Mark a notification as read",
               description = "Sets the read flag on a single notification and returns the updated notification object.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification marked as read"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal JwtUser principal,
            @PathVariable String id) {
        return ResponseEntity.ok(
                notificationService.markAsRead(principal.getCompanyId(), id));
    }

    @Operation(summary = "Get unread notification count",
               description = "Returns the count of unread notifications for the authenticated user. Used to display the notification badge in the UI.")
    @ApiResponse(responseCode = "200", description = "Unread count")
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.ok(
                notificationService.getUnreadCount(principal.getCompanyId(), principal.getUserId()));
    }
}

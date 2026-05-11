package com.inventalert.notificationService.controller;

import com.inventalert.notificationService.dto.response.NotificationResponse;
import com.inventalert.notificationService.dto.response.UnreadCountResponse;
import com.inventalert.notificationService.security.model.JwtUser;
import com.inventalert.notificationService.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal JwtUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                notificationService.getNotifications(principal.getCompanyId(), principal.getUserId(), page, size));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal JwtUser principal,
            @PathVariable String id) {
        return ResponseEntity.ok(
                notificationService.markAsRead(principal.getCompanyId(), id));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.ok(
                notificationService.getUnreadCount(principal.getCompanyId(), principal.getUserId()));
    }
}

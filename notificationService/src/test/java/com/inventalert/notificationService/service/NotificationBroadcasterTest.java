package com.inventalert.notificationService.service;

import com.inventalert.notificationService.dto.response.NotificationResponse;
import com.inventalert.notificationService.model.Notification;
import com.inventalert.notificationService.model.NotificationType;
import com.inventalert.notificationService.service.impl.NotificationBroadcasterImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationBroadcasterTest {

    @Mock SimpMessagingTemplate messagingTemplate;

    private NotificationBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new NotificationBroadcasterImpl(messagingTemplate);
    }

    @Test
    void Broadcast_CheckIfSentToCorrectDestinationTest() {
        Notification notification = buildNotification("notif-001", "konga-001", "adebayo-001",
                NotificationType.RESTOCK_ALERT, "Low stock: Indomie noodles", "product-001");

        broadcaster.broadcast(notification);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/notifications/konga-001/adebayo-001"),
                org.mockito.ArgumentMatchers.any(NotificationResponse.class)
        );
    }

    @Test
    void Broadcast_CheckIfPayloadFieldsMatchNotificationTest() {
        Instant now = Instant.now();
        Notification notification = Notification.builder()
                .notificationId("notif-002")
                .companyId("stanbic-001")
                .userId("ngozi-002")
                .type(NotificationType.TRANSFER_APPROVED)
                .message("Transfer approved for Apapa warehouse")
                .referenceId("transfer-002")
                .read(false)
                .createdAt(now)
                .build();

        ArgumentCaptor<NotificationResponse> captor = ArgumentCaptor.forClass(NotificationResponse.class);

        broadcaster.broadcast(notification);

        verify(messagingTemplate).convertAndSend(anyString(), captor.capture());
        NotificationResponse payload = captor.getValue();

        assertThat(payload.notificationId()).isEqualTo("notif-002");
        assertThat(payload.companyId()).isEqualTo("stanbic-001");
        assertThat(payload.userId()).isEqualTo("ngozi-002");
        assertThat(payload.type()).isEqualTo(NotificationType.TRANSFER_APPROVED);
        assertThat(payload.message()).isEqualTo("Transfer approved for Apapa warehouse");
        assertThat(payload.read()).isFalse();
        assertThat(payload.createdAt()).isEqualTo(now);
    }

    private Notification buildNotification(String id, String companyId, String userId,
                                           NotificationType type, String message, String referenceId) {
        return Notification.builder()
                .notificationId(id).companyId(companyId).userId(userId)
                .type(type).message(message).referenceId(referenceId)
                .read(false).createdAt(Instant.now())
                .build();
    }
}

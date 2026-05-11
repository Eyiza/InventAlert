package com.inventalert.notificationService.service;

import com.inventalert.notificationService.dto.response.NotificationResponse;
import com.inventalert.notificationService.dto.response.UnreadCountResponse;
import com.inventalert.notificationService.exception.NotificationNotFoundException;
import com.inventalert.notificationService.model.Notification;
import com.inventalert.notificationService.model.NotificationType;
import com.inventalert.notificationService.model.Notification;
import com.inventalert.notificationService.repository.RedisNotificationRepository;
import com.inventalert.notificationService.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private RedisNotificationRepository repository;
    @Mock private EmailService emailService;
    @Mock private NotificationBroadcaster broadcaster;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(repository, emailService, broadcaster);
    }

    @Test
    void CreateNotification_CheckIfSuccessfulTest() {
        when(repository.setEventProcessedIfAbsent("evt-001")).thenReturn(true);

        Notification result = notificationService.create(
                "evt-001", "company-1", "user-1", "adebayo@konga.ng",
                NotificationType.RESTOCK_ALERT, "Low stock on Indomie noodles", "alert-001");

        assertThat(result).isNotNull();
        assertThat(result.getNotificationId()).isNotBlank();
        assertThat(result.getCompanyId()).isEqualTo("company-1");
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getType()).isEqualTo(NotificationType.RESTOCK_ALERT);
        assertThat(result.getMessage()).isEqualTo("Low stock on Indomie noodles");
        assertThat(result.getReferenceId()).isEqualTo("alert-001");
        assertThat(result.isRead()).isFalse();
        assertThat(result.getCreatedAt()).isNotNull();

        verify(repository).saveHash(result);
        verify(repository).addToUserSortedSet(
                eq("company-1"), eq("user-1"), eq(result.getNotificationId()), anyDouble());
        verify(repository).incrementUnreadCount("company-1", "user-1");
    }

    @Test
    void CreateNotification_CheckIfEmailSentTest() {
        when(repository.setEventProcessedIfAbsent("evt-mail-001")).thenReturn(true);

        notificationService.create(
                "evt-mail-001", "company-1", "user-1", "adebayo@konga.ng",
                NotificationType.RESTOCK_ALERT, "Low stock on Indomie noodles", "alert-001");

        verify(emailService).sendNotificationEmail(
                "adebayo@konga.ng", "RESTOCK_ALERT", "Low stock on Indomie noodles");
    }

    @Test
    void CreateNotification_CheckIfBroadcastedTest() {
        when(repository.setEventProcessedIfAbsent("evt-ws-001")).thenReturn(true);

        notificationService.create(
                "evt-ws-001", "company-1", "user-1", "adebayo@konga.ng",
                NotificationType.RESTOCK_ALERT, "Low stock on Indomie noodles", "alert-001");

        verify(broadcaster).broadcast(any(Notification.class));
    }

    @Test
    void CreateNotification_NullEmail_CheckIfEmailNotSentTest() {
        when(repository.setEventProcessedIfAbsent("evt-no-email")).thenReturn(true);

        notificationService.create(
                "evt-no-email", "company-1", "user-1", null,
                NotificationType.TRANSFER_SUGGESTION, "Transfer suggested for Dangote cement", "ts-001");

        verifyNoInteractions(emailService);
    }

    @Test
    void CreateNotification_DuplicateEvent_CheckIfReturnsNullTest() {
        when(repository.setEventProcessedIfAbsent("evt-dup")).thenReturn(false);

        Notification result = notificationService.create(
                "evt-dup", "company-1", "user-1", "adebayo@konga.ng",
                NotificationType.RESTOCK_ALERT, "Low stock on Indomie noodles", "alert-001");

        assertThat(result).isNull();
        verify(repository, never()).saveHash(any());
        verify(repository, never()).addToUserSortedSet(any(), any(), any(), anyDouble());
        verify(repository, never()).incrementUnreadCount(any(), any());
        verifyNoInteractions(emailService);
        verifyNoInteractions(broadcaster);
    }

    @Test
    void CreateNotification_MultipleCalls_CheckIfIdsAreUniqueTest() {
        when(repository.setEventProcessedIfAbsent(any())).thenReturn(true);

        Notification first = notificationService.create(
                "evt-001", "company-1", "user-1", "adebayo@konga.ng",
                NotificationType.TRANSFER_SUGGESTION, "Transfer suggested for Dangote cement", "ts-001");
        Notification second = notificationService.create(
                "evt-002", "company-1", "user-1", "adebayo@konga.ng",
                NotificationType.TRANSFER_SUGGESTION, "Transfer suggested for Dangote cement", "ts-002");

        assertThat(first.getNotificationId()).isNotEqualTo(second.getNotificationId());
    }

    @Test
    void CreateNotification_CheckIfScoreMatchesCreatedAtEpochTest() {
        when(repository.setEventProcessedIfAbsent("evt-003")).thenReturn(true);

        ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);

        notificationService.create(
                "evt-003", "company-1", "user-1", "ngozi@stanbic.ng",
                NotificationType.RECONCILIATION_REQUESTED, "Reconciliation pending for Lagos warehouse", "rec-001");

        verify(repository).saveHash(notifCaptor.capture());
        verify(repository).addToUserSortedSet(any(), any(), any(), scoreCaptor.capture());

        Notification saved = notifCaptor.getValue();
        assertThat(scoreCaptor.getValue()).isEqualTo((double) saved.getCreatedAt().toEpochMilli());
    }

    @Test
    void GetNotifications_CheckIfReturnsMappedListTest() {
        Instant now = Instant.now();
        Set<String> ids = new LinkedHashSet<>(List.of("notif-1", "notif-2"));

        Map<String, String> hash1 = buildHash("notif-1", "company-1", "user-1",
                "RESTOCK_ALERT", "Low stock on Indomie noodles", "alert-001", "0", now);
        Map<String, String> hash2 = buildHash("notif-2", "company-1", "user-1",
                "TRANSFER_SUGGESTION", "Transfer suggested for Dangote cement", "ts-001", "1", now);

        when(repository.getNotificationIds("company-1", "user-1", 0L, 19L)).thenReturn(ids);
        when(repository.getHash("company-1", "notif-1")).thenReturn(hash1);
        when(repository.getHash("company-1", "notif-2")).thenReturn(hash2);

        List<NotificationResponse> result = notificationService.getNotifications("company-1", "user-1", 0, 20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).notificationId()).isEqualTo("notif-1");
        assertThat(result.get(0).read()).isFalse();
        assertThat(result.get(1).notificationId()).isEqualTo("notif-2");
        assertThat(result.get(1).read()).isTrue();
    }

    @Test
    void GetNotifications_EmptySet_CheckIfReturnsEmptyListTest() {
        when(repository.getNotificationIds("company-1", "user-1", 0L, 19L)).thenReturn(Set.of());

        List<NotificationResponse> result = notificationService.getNotifications("company-1", "user-1", 0, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void MarkAsRead_UnreadNotification_CheckIfSuccessfulTest() {
        Instant now = Instant.now();
        Map<String, String> hash = buildHash("notif-1", "company-1", "user-1",
                "RESTOCK_ALERT", "Low stock on Indomie noodles", "alert-001", "0", now);

        when(repository.getHash("company-1", "notif-1")).thenReturn(hash);

        NotificationResponse result = notificationService.markAsRead("company-1", "notif-1");

        assertThat(result.read()).isTrue();
        assertThat(result.notificationId()).isEqualTo("notif-1");
        verify(repository).markHashAsRead("company-1", "notif-1");
        verify(repository).decrementUnreadCount("company-1", "user-1");
    }

    @Test
    void MarkAsRead_AlreadyRead_CheckIfSkipsUpdateTest() {
        Instant now = Instant.now();
        Map<String, String> hash = buildHash("notif-1", "company-1", "user-1",
                "RESTOCK_ALERT", "Low stock on Indomie noodles", "alert-001", "1", now);

        when(repository.getHash("company-1", "notif-1")).thenReturn(hash);

        NotificationResponse result = notificationService.markAsRead("company-1", "notif-1");

        assertThat(result.read()).isTrue();
        verify(repository, never()).markHashAsRead(any(), any());
        verify(repository, never()).decrementUnreadCount(any(), any());
    }

    @Test
    void MarkAsRead_NotFound_CheckIfThrowsExceptionTest() {
        when(repository.getHash("company-1", "ghost-id")).thenReturn(Map.of());

        assertThatThrownBy(() -> notificationService.markAsRead("company-1", "ghost-id"))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining("ghost-id");

        verify(repository, never()).markHashAsRead(any(), any());
    }

    @Test
    void GetUnreadCount_CheckIfReturnsCountTest() {
        when(repository.getUnreadCount("company-1", "user-1")).thenReturn(7L);

        UnreadCountResponse result = notificationService.getUnreadCount("company-1", "user-1");

        assertThat(result.count()).isEqualTo(7L);
    }

    private Map<String, String> buildHash(String notificationId, String companyId, String userId,
                                          String type, String message, String referenceId,
                                          String isRead, Instant createdAt) {
        Map<String, String> hash = new HashMap<>();
        hash.put("notificationId", notificationId);
        hash.put("companyId",      companyId);
        hash.put("userId",         userId);
        hash.put("type",           type);
        hash.put("message",        message);
        hash.put("referenceId",    referenceId);
        hash.put("isRead",         isRead);
        hash.put("createdAt",      createdAt.toString());
        return hash;
    }
}

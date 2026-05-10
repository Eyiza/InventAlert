package com.inventalert.notificationService.service;

import com.inventalert.notificationService.model.Notification;
import com.inventalert.notificationService.model.NotificationType;
import com.inventalert.notificationService.repository.RedisNotificationRepository;
import com.inventalert.notificationService.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private RedisNotificationRepository repository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(repository);
    }

    @Test
    void CreateNotification_CheckIfSuccessfulTest() {
        when(repository.setEventProcessedIfAbsent("evt-001")).thenReturn(true);

        Notification result = notificationService.create(
                "evt-001", "company-1", "user-1",
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
    void CreateNotification_DuplicateEvent_CheckIfReturnsNullTest() {
        when(repository.setEventProcessedIfAbsent("evt-dup")).thenReturn(false);

        Notification result = notificationService.create(
                "evt-dup", "company-1", "user-1",
                NotificationType.RESTOCK_ALERT, "Low stock on Indomie noodles", "alert-001");

        assertThat(result).isNull();
        verify(repository, never()).saveHash(any());
        verify(repository, never()).addToUserSortedSet(any(), any(), any(), anyDouble());
        verify(repository, never()).incrementUnreadCount(any(), any());
    }

    @Test
    void CreateNotification_MultipleCalls_CheckIfIdsAreUniqueTest() {
        when(repository.setEventProcessedIfAbsent(any())).thenReturn(true);

        Notification first = notificationService.create(
                "evt-001", "company-1", "user-1",
                NotificationType.TRANSFER_SUGGESTION, "Transfer suggested for Dangote cement", "ts-001");
        Notification second = notificationService.create(
                "evt-002", "company-1", "user-1",
                NotificationType.TRANSFER_SUGGESTION, "Transfer suggested for Dangote cement", "ts-002");

        assertThat(first.getNotificationId()).isNotEqualTo(second.getNotificationId());
    }

    @Test
    void CreateNotification_CheckIfScoreMatchesCreatedAtEpochTest() {
        when(repository.setEventProcessedIfAbsent("evt-003")).thenReturn(true);

        ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);

        notificationService.create(
                "evt-003", "company-1", "user-1",
                NotificationType.RECONCILIATION_REQUESTED, "Reconciliation pending for Lagos warehouse", "rec-001");

        verify(repository).saveHash(notifCaptor.capture());
        verify(repository).addToUserSortedSet(any(), any(), any(), scoreCaptor.capture());

        Notification saved = notifCaptor.getValue();
        assertThat(scoreCaptor.getValue()).isEqualTo((double) saved.getCreatedAt().toEpochMilli());
    }
}

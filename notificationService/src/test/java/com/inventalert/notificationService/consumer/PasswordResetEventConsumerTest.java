package com.inventalert.notificationService.consumer;

import com.inventalert.notificationService.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetEventConsumerTest {

    @Mock EmailService emailService;

    private PasswordResetEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PasswordResetEventConsumer(emailService, "http://localhost:5173");
    }

    @Test
    void Consume_ValidEvent_CheckIfResetEmailSentTest() {
        String message = """
                {
                  "eventId": "evt-reset-001",
                  "userId": "user-abc",
                  "email": "tunde@techwave.ng",
                  "token": "abc123resettoken",
                  "expiresAt": "2026-05-12T11:00:00"
                }
                """;

        consumer.consume(message);

        ArgumentCaptor<String> toCaptor      = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor    = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendNotificationEmail(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("tunde@techwave.ng");
        assertThat(subjectCaptor.getValue()).isEqualTo("Reset your InventAlert password");
        assertThat(bodyCaptor.getValue()).contains("http://localhost:5173/reset-password?token=abc123resettoken");
        assertThat(bodyCaptor.getValue()).contains("2026-05-12T11:00:00");
    }

    @Test
    void Consume_MalformedJson_CheckIfNoExceptionThrownTest() {
        assertDoesNotThrow(() -> consumer.consume("{not: valid json"));
        verifyNoInteractions(emailService);
    }
}

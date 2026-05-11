package com.inventalert.notificationService.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventalert.notificationService.model.NotificationType;
import com.inventalert.notificationService.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock NotificationService notificationService;

    private NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationEventConsumer(notificationService, new ObjectMapper());
    }

    @Test
    void Consume_ValidRestockAlertEvent_CheckIfNotificationCreatedTest() {
        String message = """
                {
                  "eventId": "evt-konga-001",
                  "companyId": "konga-001",
                  "userId": "adebayo-001",
                  "userEmail": "adebayo@konga.ng",
                  "type": "RESTOCK_ALERT",
                  "message": "Low stock: Indomie noodles (5 units left)",
                  "referenceId": "product-indomie-001"
                }
                """;

        consumer.consume(message);

        verify(notificationService).create(
                "evt-konga-001", "konga-001", "adebayo-001", "adebayo@konga.ng",
                NotificationType.RESTOCK_ALERT, "Low stock: Indomie noodles (5 units left)",
                "product-indomie-001"
        );
    }

    @Test
    void Consume_ValidTransferSuggestionEvent_CheckIfNotificationCreatedTest() {
        String message = """
                {
                  "eventId": "evt-transfer-002",
                  "companyId": "fidelity-001",
                  "userId": "chukwuemeka-002",
                  "userEmail": "chukwuemeka@fidelity.ng",
                  "type": "TRANSFER_SUGGESTION",
                  "message": "Transfer 50 units of garri from Apapa to Ikeja warehouse",
                  "referenceId": "transfer-req-002"
                }
                """;

        consumer.consume(message);

        verify(notificationService).create(
                "evt-transfer-002", "fidelity-001", "chukwuemeka-002", "chukwuemeka@fidelity.ng",
                NotificationType.TRANSFER_SUGGESTION,
                "Transfer 50 units of garri from Apapa to Ikeja warehouse",
                "transfer-req-002"
        );
    }

    @Test
    void Consume_MalformedJson_CheckIfNoExceptionThrownTest() {
        assertDoesNotThrow(() -> consumer.consume("{not: valid json"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void Consume_EventWithUnknownType_CheckIfNoExceptionThrownTest() {
        String message = """
                {
                  "eventId": "evt-003",
                  "companyId": "stanbic-001",
                  "userId": "ngozi-003",
                  "userEmail": "ngozi@stanbic.ng",
                  "type": "UNKNOWN_EVENT_TYPE",
                  "message": "Some message",
                  "referenceId": "ref-003"
                }
                """;

        assertDoesNotThrow(() -> consumer.consume(message));
        verifyNoInteractions(notificationService);
    }
}

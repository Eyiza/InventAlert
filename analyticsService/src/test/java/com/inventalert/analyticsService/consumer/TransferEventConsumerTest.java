package com.inventalert.analyticsService.consumer;

import com.inventalert.analyticsService.dto.event.TransferEvent;
import com.inventalert.analyticsService.service.AnalyticsIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferEventConsumerTest {

    @Mock
    AnalyticsIngestionService ingestionService;

    @InjectMocks
    TransferEventConsumer consumer;

    @Test
    void consume_suggestedEvent_parsesStatusCorrectly() {
        String json = buildTransferJson("evt-1", "SUGGESTED", 50, 15.5);

        consumer.consume(json, "transfer.suggestion.created");

        TransferEvent captured = captureTransferEvent();
        assertThat(captured.eventId()).isEqualTo("evt-1");
        assertThat(captured.status()).isEqualTo("SUGGESTED");
        assertThat(captured.quantity()).isEqualTo(50);
        assertThat(captured.distanceKm()).isEqualTo(15.5);
    }

    @Test
    void consume_approvedEvent_parsesStatusCorrectly() {
        String json = buildTransferJson("evt-2", "APPROVED", null, null);

        consumer.consume(json, "transfer.approved");

        TransferEvent captured = captureTransferEvent();
        assertThat(captured.status()).isEqualTo("APPROVED");
        assertThat(captured.quantity()).isNull();
        assertThat(captured.distanceKm()).isNull();
    }

    @Test
    void consume_rejectedEvent_parsesStatusCorrectly() {
        String json = buildTransferJson("evt-3", "REJECTED", null, null);

        consumer.consume(json, "transfer.rejected");

        TransferEvent captured = captureTransferEvent();
        assertThat(captured.status()).isEqualTo("REJECTED");
    }

    @Test
    void consume_acceptedEvent_parsesStatusCorrectly() {
        String json = buildTransferJson("evt-4", "ACCEPTED", null, null);

        consumer.consume(json, "transfer.accepted");

        TransferEvent captured = captureTransferEvent();
        assertThat(captured.status()).isEqualTo("ACCEPTED");
    }

    @Test
    void consume_malformedJson_doesNotThrow_doesNotCallIngestion() {
        consumer.consume("{ bad json", "transfer.approved");

        verify(ingestionService, never()).ingestTransferEvent(any());
    }

    private TransferEvent captureTransferEvent() {
        ArgumentCaptor<TransferEvent> captor = ArgumentCaptor.forClass(TransferEvent.class);
        verify(ingestionService).ingestTransferEvent(captor.capture());
        return captor.getValue();
    }

    private String buildTransferJson(String eventId, String status, Integer quantity, Double distanceKm) {
        String qty = (quantity != null) ? String.valueOf(quantity) : "null";
        String dist = (distanceKm != null) ? String.valueOf(distanceKm) : "null";
        return String.format("""
                {
                  "eventId": "%s",
                  "companyId": "co-1",
                  "suggestionId": "sug-1",
                  "productId": "prod-1",
                  "fromWarehouseId": "wh-a",
                  "toWarehouseId": "wh-b",
                  "quantity": %s,
                  "distanceKm": %s,
                  "status": "%s",
                  "timestamp": "2025-05-01T11:00:00Z"
                }
                """, eventId, qty, dist, status);
    }
}

package com.inventalert.analyticsService.consumer;

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
class CompanyEventConsumerTest {

    @Mock
    AnalyticsIngestionService ingestionService;

    @InjectMocks
    CompanyEventConsumer consumer;

    @Test
    void consumeCreated_validJson_callsIngestionService() {
        String json = """
                {
                  "eventId": "evt-1",
                  "companyId": "co-1",
                  "companyName": "Acme Corp",
                  "adminEmail": "admin@acme.com",
                  "timestamp": "2025-03-01T10:00:00Z"
                }
                """;

        consumer.consumeCreated(json);

        var captor = ArgumentCaptor.forClass(com.inventalert.analyticsService.dto.event.CompanyCreatedEvent.class);
        verify(ingestionService).ingestCompanyCreated(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo("evt-1");
        assertThat(captor.getValue().companyName()).isEqualTo("Acme Corp");
    }

    @Test
    void consumeCreated_malformedJson_doesNotThrow_doesNotCallIngestion() {
        String badJson = "{ this is not valid json }";

        consumer.consumeCreated(badJson);

        verify(ingestionService, never()).ingestCompanyCreated(any());
    }

    @Test
    void consumeCreated_emptyString_doesNotThrow() {
        consumer.consumeCreated("");

        verify(ingestionService, never()).ingestCompanyCreated(any());
    }

    @Test
    void consumeOffboarded_validJson_callsIngestionService() {
        String json = """
                {
                  "eventId": "evt-2",
                  "companyId": "co-1",
                  "timestamp": "2025-06-01T08:00:00Z"
                }
                """;

        consumer.consumeOffboarded(json);

        var captor = ArgumentCaptor.forClass(com.inventalert.analyticsService.dto.event.CompanyOffboardedEvent.class);
        verify(ingestionService).ingestCompanyOffboarded(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo("evt-2");
        assertThat(captor.getValue().companyId()).isEqualTo("co-1");
    }

    @Test
    void consumeOffboarded_malformedJson_doesNotThrow_doesNotCallIngestion() {
        consumer.consumeOffboarded("NOT JSON");

        verify(ingestionService, never()).ingestCompanyOffboarded(any());
    }
}

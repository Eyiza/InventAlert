package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.model.DistanceSource;
import com.inventalert.inventoryService.service.impl.GoogleMapsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GoogleMapsServiceTest {

    @InjectMocks
    private GoogleMapsServiceImpl googleMapsService;

    @Test
    void haversine_returnsCorrectDistanceWhenNoApiKey() {
        ReflectionTestUtils.setField(googleMapsService, "apiKey", "");
        ReflectionTestUtils.setField(googleMapsService, "timeoutSeconds", 3);

        // New York to Los Angeles approximately 3940 km straight-line
        DistanceResult result = googleMapsService.getDrivingDistanceKm(
                "nyc", new BigDecimal("40.7128"), new BigDecimal("-74.0060"),
                "lax", new BigDecimal("34.0522"), new BigDecimal("-118.2437")
        );

        assertThat(result.km()).isGreaterThan(3500.0).isLessThan(4500.0);
        assertThat(result.source()).isEqualTo(DistanceSource.HAVERSINE);
    }

    @Test
    void haversine_sameLocationReturnsZero() {
        ReflectionTestUtils.setField(googleMapsService, "apiKey", "");
        ReflectionTestUtils.setField(googleMapsService, "timeoutSeconds", 3);

        DistanceResult result = googleMapsService.getDrivingDistanceKm(
                "w1", new BigDecimal("40.7128"), new BigDecimal("-74.0060"),
                "w1", new BigDecimal("40.7128"), new BigDecimal("-74.0060")
        );

        assertThat(result.km()).isEqualByComparingTo(0.0);
        assertThat(result.source()).isEqualTo(DistanceSource.HAVERSINE);
    }

    @Test
    void haversine_usedAsFallbackWhenApiKeyBlank() {
        ReflectionTestUtils.setField(googleMapsService, "apiKey", "   ");
        ReflectionTestUtils.setField(googleMapsService, "timeoutSeconds", 3);

        DistanceResult result = googleMapsService.getDrivingDistanceKm(
                "w1", new BigDecimal("6.5244"), new BigDecimal("3.3792"),
                "w2", new BigDecimal("9.0820"), new BigDecimal("8.6753")
        );

        assertThat(result.source()).isEqualTo(DistanceSource.HAVERSINE);
        assertThat(result.km()).isPositive();
    }
}

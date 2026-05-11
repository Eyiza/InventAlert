package com.inventalert.inventoryService.service;

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

        // New York to Los Angeles approximately 3940 km
        Double distance = googleMapsService.getDrivingDistanceKm(
                "nyc", new BigDecimal("40.7128"), new BigDecimal("-74.0060"),
                "lax", new BigDecimal("34.0522"), new BigDecimal("-118.2437")
        );

        assertThat(distance).isGreaterThan(3500.0).isLessThan(4500.0);
    }

    @Test
    void haversine_sameLocationReturnsZero() {
        ReflectionTestUtils.setField(googleMapsService, "apiKey", "");
        ReflectionTestUtils.setField(googleMapsService, "timeoutSeconds", 3);

        Double distance = googleMapsService.getDrivingDistanceKm(
                "w1", new BigDecimal("40.7128"), new BigDecimal("-74.0060"),
                "w1", new BigDecimal("40.7128"), new BigDecimal("-74.0060")
        );

        assertThat(distance).isEqualByComparingTo(0.0);
    }
}

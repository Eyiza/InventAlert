package com.inventalert.inventoryService.service;

import com.inventalert.inventoryService.dto.response.RestockAlertResponse;
import com.inventalert.inventoryService.exception.AlertNotFoundException;
import com.inventalert.inventoryService.exception.InvalidStateTransitionException;
import com.inventalert.inventoryService.model.AlertStatus;
import com.inventalert.inventoryService.model.RestockAlert;
import com.inventalert.inventoryService.model.StockLevel;
import com.inventalert.inventoryService.repository.RestockAlertRepository;
import com.inventalert.inventoryService.repository.StockLevelRepository;
import com.inventalert.inventoryService.service.impl.RestockAlertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestockAlertServiceTest {

    @Mock
    private RestockAlertRepository alertRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private com.inventalert.inventoryService.kafka.AlertEventProducer alertEventProducer;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RestockAlertServiceImpl alertService;

    private RestockAlert openAlert;

    @BeforeEach
    void setUp() {
        openAlert = RestockAlert.builder()
                .id("a1").productId("p1").warehouseId("w1")
                .stockAtAlert(5).threshold(10).status(AlertStatus.OPEN).build();
    }

    @Test
    void createAlert_skipsDuplicateOpenAlert() {
        when(alertRepository.existsByProductIdAndWarehouseIdAndStatus("p1", "w1", AlertStatus.OPEN))
                .thenReturn(true);

        alertService.createAlert("p1", "w1", 5, 10, "company1");

        verify(alertRepository, never()).save(any());
        verify(alertEventProducer, never()).publishAlertCreated(any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void createAlert_savesAndPublishesWhenNoDuplicate() {
        when(alertRepository.existsByProductIdAndWarehouseIdAndStatus("p1", "w1", AlertStatus.OPEN))
                .thenReturn(false);
        when(alertRepository.save(any())).thenReturn(openAlert);

        alertService.createAlert("p1", "w1", 5, 10, "company1");

        verify(alertRepository).save(any());
        verify(alertEventProducer).publishAlertCreated(any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void acknowledge_transitionsOpenToAcknowledged() {
        when(alertRepository.findById("a1")).thenReturn(Optional.of(openAlert));
        when(alertRepository.save(any())).thenReturn(openAlert);

        alertService.acknowledge("a1", "user1");

        assertThat(openAlert.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    }

    @Test
    void acknowledge_throwsWhenNotOpen() {
        openAlert.setStatus(AlertStatus.ACKNOWLEDGED);
        when(alertRepository.findById("a1")).thenReturn(Optional.of(openAlert));

        assertThatThrownBy(() -> alertService.acknowledge("a1", "user1"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void markOrderPlaced_transitionsAcknowledgedToOrderPlaced() {
        openAlert.setStatus(AlertStatus.ACKNOWLEDGED);
        when(alertRepository.findById("a1")).thenReturn(Optional.of(openAlert));
        when(alertRepository.save(any())).thenReturn(openAlert);

        alertService.markOrderPlaced("a1", "user1");

        assertThat(openAlert.getStatus()).isEqualTo(AlertStatus.ORDER_PLACED);
    }

    @Test
    void resolve_transitionsOrderPlacedToResolved() {
        openAlert.setStatus(AlertStatus.ORDER_PLACED);
        when(alertRepository.findById("a1")).thenReturn(Optional.of(openAlert));
        StockLevel level = StockLevel.builder()
                .productId("p1").warehouseId("w1")
                .currentStock(50).threshold(10).velocityPerDay(BigDecimal.ZERO).build();
        when(stockLevelRepository.findByProductIdAndWarehouseId("p1", "w1")).thenReturn(Optional.of(level));
        when(alertRepository.save(any())).thenReturn(openAlert);

        alertService.resolve("a1", "user1");

        assertThat(openAlert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    }

    @Test
    void resolve_throwsWhenNotOrderPlaced() {
        openAlert.setStatus(AlertStatus.OPEN);
        when(alertRepository.findById("a1")).thenReturn(Optional.of(openAlert));

        assertThatThrownBy(() -> alertService.resolve("a1", "user1"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void list_returnsAllAlerts() {
        when(alertRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(openAlert));

        List<RestockAlertResponse> result = alertService.list(null);

        assertThat(result).hasSize(1);
    }
}

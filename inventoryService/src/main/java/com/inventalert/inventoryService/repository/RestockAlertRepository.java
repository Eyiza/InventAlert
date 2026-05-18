package com.inventalert.inventoryService.repository;

import com.inventalert.inventoryService.model.AlertStatus;
import com.inventalert.inventoryService.model.RestockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestockAlertRepository extends JpaRepository<RestockAlert, String> {
    boolean existsByProductIdAndWarehouseIdAndStatus(String productId, String warehouseId, AlertStatus status);
    List<RestockAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status);
    List<RestockAlert> findAllByOrderByCreatedAtDesc();
    List<RestockAlert> findByProductIdAndWarehouseIdAndStatusNot(String productId, String warehouseId, AlertStatus status);
}

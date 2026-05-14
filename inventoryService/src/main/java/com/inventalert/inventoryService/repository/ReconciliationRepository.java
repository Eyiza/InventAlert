package com.inventalert.inventoryService.repository;

import com.inventalert.inventoryService.model.Reconciliation;
import com.inventalert.inventoryService.model.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, String> {
    List<Reconciliation> findByStatus(ReconciliationStatus status);
    List<Reconciliation> findByWarehouseId(String warehouseId);
    Page<Reconciliation> findByWarehouseId(String warehouseId, Pageable pageable);
}

package com.inventalert.inventoryService.repository;

import com.inventalert.inventoryService.model.MovementType;
import com.inventalert.inventoryService.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, String> {

    List<StockMovement> findByProductIdAndWarehouseIdAndTypeAndCreatedAtAfter(
            String productId, String warehouseId, MovementType type, LocalDateTime after);

    @Query("SELECT m FROM StockMovement m WHERE " +
            "(:productId IS NULL OR m.productId = :productId) AND " +
            "(:warehouseId IS NULL OR m.warehouseId = :warehouseId) AND " +
            "(:type IS NULL OR m.type = :type) AND " +
            "(:from IS NULL OR m.createdAt >= :from) AND " +
            "(:to IS NULL OR m.createdAt <= :to)")
    List<StockMovement> findWithFilters(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("type") MovementType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}

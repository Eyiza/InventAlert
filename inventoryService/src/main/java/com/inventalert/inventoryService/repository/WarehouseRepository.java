package com.inventalert.inventoryService.repository;

import com.inventalert.inventoryService.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
    List<Warehouse> findByIsActiveTrue();
    Optional<Warehouse> findByIdAndIsActiveTrue(String id);
}

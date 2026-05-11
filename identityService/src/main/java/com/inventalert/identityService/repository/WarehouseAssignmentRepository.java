package com.inventalert.identityService.repository;

import com.inventalert.identityService.model.WarehouseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseAssignmentRepository extends JpaRepository<WarehouseAssignment, String> {
    List<WarehouseAssignment> findAllByUserId(String userId);
    boolean existsByUserIdAndWarehouseId(String userId, String warehouseId);
    List<WarehouseAssignment> findAllByWarehouseIdAndCompanyId(String warehouseId, String companyId);
    void deleteAllByCompanyId(String companyId);
}

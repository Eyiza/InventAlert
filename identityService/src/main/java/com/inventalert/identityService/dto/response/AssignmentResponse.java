package com.inventalert.identityService.dto.response;

import com.inventalert.identityService.model.WarehouseAssignment;

import java.time.LocalDateTime;

public record AssignmentResponse(
        String id,
        String userId,
        String companyId,
        String warehouseId,
        LocalDateTime assignedAt
) {
    public static AssignmentResponse from(WarehouseAssignment a) {
        return new AssignmentResponse(
                a.getId(),
                a.getUserId(),
                a.getCompanyId(),
                a.getWarehouseId(),
                a.getAssignedAt()
        );
    }
}

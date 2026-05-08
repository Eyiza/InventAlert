package com.inventalert.identityService.dto.response;

public record LoginResponse(
        String token,
        String userId,
        String companyId,     // null for SuperAdmin
        String role,
        String warehouseId    // null unless role is WAREHOUSE_STAFF
) {}

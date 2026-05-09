package com.inventalert.identityService.dto.response;

import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;

import java.time.LocalDateTime;

public record UserResponse(
        String id,
        String companyId,
        String email,
        Role role,
        boolean isActive,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getCompanyId(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}

package com.inventalert.identityService.dto.request;

import com.inventalert.identityService.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank String email,
        @NotBlank @Size(min = 8) String password,
        @NotNull Role role,
        String warehouseId
) {}

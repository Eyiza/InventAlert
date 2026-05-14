package com.inventalert.identityService.dto.request;

import com.inventalert.identityService.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Name is required.") String name,
        @NotBlank(message = "Email address is required.") String email,
        @NotBlank(message = "Password is required.") @Size(min = 8, message = "Password must be at least 8 characters.") String password,
        @NotNull(message = "A role must be selected.") Role role,
        String warehouseId
) {}

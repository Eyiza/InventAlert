package com.inventalert.identityService.dto.request;

import com.inventalert.identityService.model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull Role role
) {}

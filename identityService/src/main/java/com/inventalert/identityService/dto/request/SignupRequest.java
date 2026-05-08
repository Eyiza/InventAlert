package com.inventalert.identityService.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String companyName,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 8) String password
) {}

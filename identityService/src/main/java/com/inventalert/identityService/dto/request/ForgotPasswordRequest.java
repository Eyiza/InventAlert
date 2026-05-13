package com.inventalert.identityService.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email address is required.") @Email(message = "Please enter a valid email address.") String email
) {}

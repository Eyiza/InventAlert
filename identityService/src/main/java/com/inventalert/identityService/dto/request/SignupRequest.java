package com.inventalert.identityService.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank(message = "Company name is required.")
    private String companyName;

    @NotBlank(message = "Email address is required.")
    @Email(message = "Please enter a valid email address.")
    private String adminEmail;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must be at least 8 characters.")
    private String password;

    private String logoUrl;
}

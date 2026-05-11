package com.inventalert.identityService.controller;

import com.inventalert.identityService.dto.request.ForgotPasswordRequest;
import com.inventalert.identityService.dto.request.LoginRequest;
import com.inventalert.identityService.dto.request.ResetPasswordRequest;
import com.inventalert.identityService.dto.request.SignupRequest;
import com.inventalert.identityService.dto.response.LoginResponse;
import com.inventalert.identityService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Public endpoints — signup, login, superadmin login")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Register a new company",
        description = "Creates a company and its first ADMIN user. Returns a signed JWT immediately. Also publishes a company.created Kafka event.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Company created, JWT returned",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content)
        }
    )
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(authService.signup(request));
    }

    @Operation(
        summary = "Login as any user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authenticated, JWT returned",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
            @ApiResponse(responseCode = "403", description = "Company is suspended", content = @Content)
        }
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
        summary = "Login as SuperAdmin",
        description = "Credentials come from SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD env vars. The returned token has no companyId.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authenticated, SuperAdmin JWT returned",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
        }
    )
    @PostMapping("/superadmin/login")
    public ResponseEntity<?> superAdminLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.superAdminLogin(request));
    }

    @Operation(
        summary = "Request a password reset",
        description = "Generates a single-use reset token (valid 1 hour) and publishes a password.reset.requested " +
                      "Kafka event carrying the token for the Notification Service to email. Always returns 200 " +
                      "regardless of whether the email is registered.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Request accepted", content = @Content),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
        }
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Reset password using a token",
        description = "Consumes the single-use token issued by forgot-password, updates the user's password, " +
                      "and marks the token as used. Returns 400 if the token is invalid, expired, or already used.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Password updated", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid, expired, or already-used token", content = @Content)
        }
    )
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}

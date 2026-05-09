package com.inventalert.identityService.controller;

import com.inventalert.identityService.dto.request.AssignWarehouseRequest;
import com.inventalert.identityService.dto.request.CreateUserRequest;
import com.inventalert.identityService.dto.request.UpdateRoleRequest;
import com.inventalert.identityService.dto.response.AssignmentResponse;
import com.inventalert.identityService.dto.response.UserResponse;
import com.inventalert.identityService.security.model.JwtUser;
import com.inventalert.identityService.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "User management — ADMIN role required")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
        summary = "Create a user under the authenticated company",
        responses = {
            @ApiResponse(responseCode = "201", description = "User created",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content)
        }
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @AuthenticationPrincipal JwtUser principal,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(201).body(userService.createUser(principal.getCompanyId(), request));
    }

    @Operation(
        summary = "List all users in the authenticated company",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of users"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content)
        }
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers(
            @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.ok(userService.listUsers(principal.getCompanyId()));
    }

    @Operation(
        summary = "Change a user's role",
        responses = {
            @ApiResponse(responseCode = "200", description = "Role updated",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
        }
    )
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRole(
            @AuthenticationPrincipal JwtUser principal,
            @Parameter(description = "UUID of the user") @PathVariable String id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.updateRole(principal.getCompanyId(), id, request));
    }

    @Operation(
        summary = "Deactivate a user",
        description = "Sets isActive to false. Returns 409 if already deactivated.",
        responses = {
            @ApiResponse(responseCode = "200", description = "User deactivated",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "User already deactivated", content = @Content)
        }
    )
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deactivateUser(
            @AuthenticationPrincipal JwtUser principal,
            @Parameter(description = "UUID of the user") @PathVariable String id) {
        return ResponseEntity.ok(userService.deactivateUser(principal.getCompanyId(), id));
    }

    @Operation(
        summary = "Assign a user to a warehouse",
        responses = {
            @ApiResponse(responseCode = "201", description = "Assignment created",
                content = @Content(schema = @Schema(implementation = AssignmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
        }
    )
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssignmentResponse> assignToWarehouse(
            @AuthenticationPrincipal JwtUser principal,
            @Parameter(description = "UUID of the user") @PathVariable String id,
            @Valid @RequestBody AssignWarehouseRequest request) {
        return ResponseEntity.status(201).body(userService.assignToWarehouse(principal.getCompanyId(), id, request));
    }

    @Operation(
        summary = "Get all warehouse assignments for a user",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of assignments"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
        }
    )
    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AssignmentResponse>> getAssignments(
            @AuthenticationPrincipal JwtUser principal,
            @Parameter(description = "UUID of the user") @PathVariable String id) {
        return ResponseEntity.ok(userService.getAssignments(principal.getCompanyId(), id));
    }
}

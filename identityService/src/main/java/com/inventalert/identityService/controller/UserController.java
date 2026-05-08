package com.inventalert.identityService.controller;

import com.inventalert.identityService.dto.request.AssignWarehouseRequest;
import com.inventalert.identityService.dto.request.CreateUserRequest;
import com.inventalert.identityService.dto.request.UpdateRoleRequest;
import com.inventalert.identityService.dto.response.AssignmentResponse;
import com.inventalert.identityService.dto.response.UserResponse;
import com.inventalert.identityService.security.model.JwtUser;
import com.inventalert.identityService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @AuthenticationPrincipal JwtUser principal,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(201).body(userService.createUser(principal.getCompanyId(), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers(
            @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.ok(userService.listUsers(principal.getCompanyId()));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRole(
            @AuthenticationPrincipal JwtUser principal,
            @PathVariable String id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.updateRole(principal.getCompanyId(), id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deactivateUser(
            @AuthenticationPrincipal JwtUser principal,
            @PathVariable String id) {
        return ResponseEntity.ok(userService.deactivateUser(principal.getCompanyId(), id));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssignmentResponse> assignToWarehouse(
            @AuthenticationPrincipal JwtUser principal,
            @PathVariable String id,
            @Valid @RequestBody AssignWarehouseRequest request) {
        return ResponseEntity.status(201).body(userService.assignToWarehouse(principal.getCompanyId(), id, request));
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AssignmentResponse>> getAssignments(
            @AuthenticationPrincipal JwtUser principal,
            @PathVariable String id) {
        return ResponseEntity.ok(userService.getAssignments(principal.getCompanyId(), id));
    }
}

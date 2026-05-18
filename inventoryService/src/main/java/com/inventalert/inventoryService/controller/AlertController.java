package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.response.RestockAlertResponse;
import com.inventalert.inventoryService.model.AlertStatus;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.RestockAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Restock Alerts", description = "View and action restock alerts through their OPEN → ACKNOWLEDGED → ORDER_PLACED → RESOLVED lifecycle")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final RestockAlertService alertService;

    @Operation(summary = "List restock alerts", description = "Returns restock alerts for the company. Filter by status: OPEN, ACKNOWLEDGED, ORDER_PLACED, RESOLVED. Accessible to PROCUREMENT_OFFICER and MANAGER.")
    @ApiResponse(responseCode = "200", description = "Alert list")
    @GetMapping
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER')")
    public ResponseEntity<List<RestockAlertResponse>> list(
            @RequestParam(required = false) AlertStatus status) {
        return ResponseEntity.ok(alertService.list(status));
    }

    @Operation(summary = "Acknowledge an alert", description = "Moves alert from OPEN to ACKNOWLEDGED. Requires PROCUREMENT_OFFICER role.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Alert acknowledged"),
        @ApiResponse(responseCode = "404", description = "Alert not found"),
        @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('PROCUREMENT_OFFICER')")
    public ResponseEntity<Void> acknowledge(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        alertService.acknowledge(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark order placed", description = "Moves alert from ACKNOWLEDGED to ORDER_PLACED, confirming a purchase order has been raised.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Order marked as placed"),
        @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    @PatchMapping("/{id}/order-placed")
    @PreAuthorize("hasRole('PROCUREMENT_OFFICER')")
    public ResponseEntity<Void> markOrderPlaced(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        alertService.markOrderPlaced(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resolve an alert", description = "Closes the alert once stock has been replenished. Moves state to RESOLVED.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Alert resolved"),
        @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('PROCUREMENT_OFFICER')")
    public ResponseEntity<Void> resolve(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        alertService.resolve(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}

package com.inventalert.inventoryService.controller;

import com.inventalert.inventoryService.dto.response.RestockAlertResponse;
import com.inventalert.inventoryService.security.model.JwtUser;
import com.inventalert.inventoryService.service.RestockAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final RestockAlertService alertService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','MANAGER')")
    public ResponseEntity<List<RestockAlertResponse>> list() {
        return ResponseEntity.ok(alertService.list());
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('PROCUREMENT_OFFICER')")
    public ResponseEntity<Void> acknowledge(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        alertService.acknowledge(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/order-placed")
    @PreAuthorize("hasRole('PROCUREMENT_OFFICER')")
    public ResponseEntity<Void> markOrderPlaced(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        alertService.markOrderPlaced(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('PROCUREMENT_OFFICER')")
    public ResponseEntity<Void> resolve(@PathVariable String id) {
        JwtUser principal = (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        alertService.resolve(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}

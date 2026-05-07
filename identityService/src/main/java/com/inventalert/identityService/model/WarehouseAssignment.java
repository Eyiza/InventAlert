package com.inventalert.identityService.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WarehouseAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String companyId;

    @Column(nullable = false)
    private String warehouseId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime assignedAt;
}

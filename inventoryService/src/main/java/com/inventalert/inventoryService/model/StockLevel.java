package com.inventalert.inventoryService.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stockLevels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Version
    private int version;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String warehouseId;

    @Column(nullable = false)
    private int currentStock = 0;

    @Column(nullable = false)
    private int threshold = 0;

    @Column(nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal velocityPerDay = BigDecimal.ZERO;

    @Column
    private Integer daysUntilEmpty;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

CREATE TABLE stockMovements (
    id          CHAR(36)     NOT NULL,
    productId   CHAR(36)     NOT NULL,
    warehouseId CHAR(36)     NOT NULL,
    type        ENUM('INTAKE','OUTBOUND_SALE','TRANSFER_OUT','TRANSFER_IN','RECONCILIATION') NOT NULL,
    quantity    INT          NOT NULL,
    referenceId CHAR(36)     NULL,
    createdBy   CHAR(36)     NOT NULL,
    createdAt   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_movement_product_warehouse_date (productId, warehouseId, createdAt),
    INDEX idx_movement_type_date (type, createdAt),
    CONSTRAINT fk_movement_product   FOREIGN KEY (productId)   REFERENCES products (id),
    CONSTRAINT fk_movement_warehouse FOREIGN KEY (warehouseId) REFERENCES warehouses (id)
);

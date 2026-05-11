CREATE TABLE stockLevels (
    id             CHAR(36)       NOT NULL,
    productId      CHAR(36)       NOT NULL,
    warehouseId    CHAR(36)       NOT NULL,
    currentStock   INT            NOT NULL DEFAULT 0,
    threshold      INT            NOT NULL DEFAULT 0,
    velocityPerDay DECIMAL(10,4)  NOT NULL DEFAULT 0,
    daysUntilEmpty INT            NULL,
    updatedAt      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_stock_product_warehouse (productId, warehouseId),
    CONSTRAINT chk_stock_non_negative CHECK (currentStock >= 0),
    CONSTRAINT fk_stock_product   FOREIGN KEY (productId)   REFERENCES products (id),
    CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouseId) REFERENCES warehouses (id)
);

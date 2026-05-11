CREATE TABLE restockAlerts (
    id          CHAR(36)  NOT NULL,
    productId   CHAR(36)  NOT NULL,
    warehouseId CHAR(36)  NOT NULL,
    stockAtAlert INT      NOT NULL,
    threshold   INT       NOT NULL,
    status      ENUM('OPEN','ACKNOWLEDGED','ORDER_PLACED','RESOLVED') NOT NULL DEFAULT 'OPEN',
    assignedTo  CHAR(36)  NULL,
    createdAt   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_alert_product_warehouse_status (productId, warehouseId, status),
    CONSTRAINT fk_alert_product   FOREIGN KEY (productId)   REFERENCES products (id),
    CONSTRAINT fk_alert_warehouse FOREIGN KEY (warehouseId) REFERENCES warehouses (id)
);

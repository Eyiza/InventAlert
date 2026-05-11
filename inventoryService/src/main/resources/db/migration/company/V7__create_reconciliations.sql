CREATE TABLE reconciliations (
    id            CHAR(36)  NOT NULL,
    productId     CHAR(36)  NOT NULL,
    warehouseId   CHAR(36)  NOT NULL,
    systemCount   INT       NOT NULL,
    physicalCount INT       NOT NULL,
    discrepancy   INT       NOT NULL,
    reason        TEXT      NOT NULL,
    status        ENUM('PENDING_APPROVAL','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING_APPROVAL',
    createdBy     CHAR(36)  NOT NULL,
    approvedBy    CHAR(36)  NULL,
    createdAt     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_recon_product   FOREIGN KEY (productId)   REFERENCES products (id),
    CONSTRAINT fk_recon_warehouse FOREIGN KEY (warehouseId) REFERENCES warehouses (id)
);

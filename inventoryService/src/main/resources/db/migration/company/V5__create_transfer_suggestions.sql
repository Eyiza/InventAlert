CREATE TABLE transferSuggestions (
    id              CHAR(36)      NOT NULL,
    productId       CHAR(36)      NOT NULL,
    fromWarehouseId CHAR(36)      NOT NULL,
    toWarehouseId   CHAR(36)      NOT NULL,
    quantity        INT           NOT NULL,
    distanceKm      DECIMAL(10,2) NULL,
    distanceSource  ENUM('GOOGLE_MAPS','HAVERSINE') NOT NULL DEFAULT 'GOOGLE_MAPS',
    status          ENUM('SUGGESTED','APPROVED','REJECTED','IN_TRANSIT','COMPLETED','DELIVERY_REJECTED') NOT NULL DEFAULT 'SUGGESTED',
    approvedBy      CHAR(36)      NULL,
    createdAt       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_transfer_product        FOREIGN KEY (productId)       REFERENCES products (id),
    CONSTRAINT fk_transfer_from_warehouse FOREIGN KEY (fromWarehouseId) REFERENCES warehouses (id),
    CONSTRAINT fk_transfer_to_warehouse   FOREIGN KEY (toWarehouseId)   REFERENCES warehouses (id)
);

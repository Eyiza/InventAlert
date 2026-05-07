CREATE TABLE WarehouseAssignment (
    id          CHAR(36)  NOT NULL,
    userId      CHAR(36)  NOT NULL,
    companyId   CHAR(36)  NOT NULL,
    warehouseId CHAR(36)  NOT NULL,
    assignedAt  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_warehouseAssignment_user    FOREIGN KEY (userId)    REFERENCES User (id),
    CONSTRAINT fk_warehouseAssignment_company FOREIGN KEY (companyId) REFERENCES Company (id)
);

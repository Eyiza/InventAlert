CREATE TABLE warehouses (
    id        CHAR(36)      NOT NULL,
    name      VARCHAR(255)  NOT NULL,
    address   TEXT          NOT NULL,
    latitude  DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    isActive  BOOLEAN       NOT NULL DEFAULT TRUE,
    createdBy CHAR(36)      NOT NULL,
    createdAt TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

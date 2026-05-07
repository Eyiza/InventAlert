CREATE TABLE User (
    id           CHAR(36)     NOT NULL,
    companyId    CHAR(36)     NOT NULL,
    email        VARCHAR(255) NOT NULL,
    passwordHash VARCHAR(255) NOT NULL,
    role         ENUM('ADMIN','MANAGER','WAREHOUSE_STAFF','PROCUREMENT_OFFICER') NOT NULL,
    isActive     BOOLEAN      NOT NULL DEFAULT TRUE,
    warehouseId  CHAR(36)     NULL,
    createdAt    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_email (email),
    CONSTRAINT fk_user_company FOREIGN KEY (companyId) REFERENCES Company (id)
);

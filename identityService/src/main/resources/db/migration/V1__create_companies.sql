CREATE TABLE Company (
    id          CHAR(36)                   NOT NULL,
    companyName VARCHAR(255)               NOT NULL,
    adminEmail  VARCHAR(255)               NOT NULL,
    status      ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    createdAt   TIMESTAMP                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_company_adminEmail (adminEmail)
);

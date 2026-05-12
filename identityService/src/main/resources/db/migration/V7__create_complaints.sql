CREATE TABLE Complaint (
    id          CHAR(36)                            NOT NULL,
    companyId   CHAR(36)                            NOT NULL,
    submittedBy CHAR(36)                            NOT NULL,
    subject     VARCHAR(255),
    description TEXT                                NOT NULL,
    status      ENUM('OPEN','UNDER_REVIEW','RESOLVED') NOT NULL DEFAULT 'OPEN',
    resolution  TEXT,
    createdAt   TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt   TIMESTAMP                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

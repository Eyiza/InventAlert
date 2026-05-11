CREATE TABLE products (
    id               CHAR(36)     NOT NULL,
    name             VARCHAR(255) NOT NULL,
    sku              VARCHAR(100) NOT NULL,
    unitOfMeasure    VARCHAR(50)  NOT NULL,
    defaultThreshold INT          NOT NULL DEFAULT 0,
    isActive         BOOLEAN      NOT NULL DEFAULT TRUE,
    createdBy        CHAR(36)     NOT NULL,
    createdAt        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_product_sku (sku)
);

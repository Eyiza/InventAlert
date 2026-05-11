CREATE TABLE password_reset_tokens (
    id        CHAR(36)     NOT NULL,
    userId    CHAR(36)     NOT NULL,
    token     VARCHAR(255) NOT NULL,
    expiresAt TIMESTAMP    NOT NULL,
    used      BOOLEAN      NOT NULL DEFAULT FALSE,
    createdAt TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_password_reset_token (token),
    CONSTRAINT fk_prt_user FOREIGN KEY (userId) REFERENCES User (id)
);

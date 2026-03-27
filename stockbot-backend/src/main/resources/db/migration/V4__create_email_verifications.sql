CREATE TABLE email_verifications (
    id          CHAR(36)  NOT NULL,
    user_id     CHAR(36)  NOT NULL,
    token       CHAR(36)  NOT NULL,
    expires_at  DATETIME  NOT NULL,
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_email_verifications_token (token),
    INDEX idx_email_verifications_user (user_id),
    CONSTRAINT fk_email_ver_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

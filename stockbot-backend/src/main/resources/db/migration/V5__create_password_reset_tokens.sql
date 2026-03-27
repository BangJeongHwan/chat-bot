CREATE TABLE password_reset_tokens (
    id          CHAR(36)  NOT NULL,
    user_id     CHAR(36)  NOT NULL,
    token       CHAR(36)  NOT NULL,
    expires_at  DATETIME  NOT NULL,
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_pw_reset_token (token),
    INDEX idx_pw_reset_user (user_id),
    CONSTRAINT fk_pw_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

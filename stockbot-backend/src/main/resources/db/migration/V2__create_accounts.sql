CREATE TABLE accounts (
    id              CHAR(36)      NOT NULL,
    user_id         CHAR(36)      NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    balance_krw     DECIMAL(18,2) NOT NULL DEFAULT 100000000.00,
    balance_usd     DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    initial_balance DECIMAL(18,2) NOT NULL,
    is_default      TINYINT(1)    NOT NULL DEFAULT 0,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

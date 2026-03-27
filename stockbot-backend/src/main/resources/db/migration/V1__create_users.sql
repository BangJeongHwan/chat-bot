CREATE TABLE users (
    id                CHAR(36)     NOT NULL,
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    nickname          VARCHAR(50),
    profile_image_url VARCHAR(500),
    provider          VARCHAR(20)  NOT NULL DEFAULT 'local',
    provider_id       VARCHAR(255),
    invest_style      VARCHAR(20),
    level             INT          NOT NULL DEFAULT 1,
    experience        INT          NOT NULL DEFAULT 0,
    is_active         TINYINT(1)   NOT NULL DEFAULT 1,
    is_verified       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

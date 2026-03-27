---
phase: 01-foundation-and-authentication
plan: 01
subsystem: backend-infrastructure
tags: [spring-boot, docker, flyway, mysql, gradle, kotlin]
dependency_graph:
  requires: []
  provides:
    - docker-compose MySQL 8.0 + Redis 7 + MailHog local dev environment
    - Spring Boot 3.5.13 Gradle Kotlin DSL project skeleton
    - Flyway 5-migration schema (users, accounts, refresh_tokens, email_verifications, password_reset_tokens)
    - /actuator/health endpoint
  affects: []
tech_stack:
  added:
    - Spring Boot 3.5.13 (Kotlin 2.2.0, Java 21)
    - Flyway 10.x + flyway-mysql
    - MySQL 8.0 (Docker)
    - Redis 7 (Docker)
    - MailHog (Docker, SMTP mock)
    - jjwt 0.13.0 (modular: api/impl/jackson)
    - Testcontainers MySQL 1.20.4
    - MockK 1.13.16
  patterns:
    - CHAR(36) for all UUID primary keys
    - DECIMAL(18,2) for KRW, DECIMAL(18,4) for USD
    - utf8mb4_unicode_ci for all tables
    - ON DELETE CASCADE for token tables referencing users
key_files:
  created:
    - docker-compose.yml
    - stockbot-backend/build.gradle.kts
    - stockbot-backend/settings.gradle.kts
    - stockbot-backend/src/main/kotlin/com/stockbot/StockbotApplication.kt
    - stockbot-backend/src/main/resources/application.yml
    - stockbot-backend/src/main/resources/application-local.yml
    - stockbot-backend/gradle/wrapper/gradle-wrapper.properties
    - stockbot-backend/src/main/resources/db/migration/V1__create_users.sql
    - stockbot-backend/src/main/resources/db/migration/V2__create_accounts.sql
    - stockbot-backend/src/main/resources/db/migration/V3__create_refresh_tokens.sql
    - stockbot-backend/src/main/resources/db/migration/V4__create_email_verifications.sql
    - stockbot-backend/src/main/resources/db/migration/V5__create_password_reset_tokens.sql
  modified: []
decisions:
  - Spring Boot 3.5.13 with Kotlin 2.2.0 and Java 21 toolchain (per CLAUDE.md constraints)
  - jjwt 0.13.0 modular split (api compile, impl+jackson runtime-only)
  - CHAR(36) UUIDs (not BINARY(16)) for human-readable debugging
  - Colima (Docker runtime on macOS) used for local dev; services start with docker compose
metrics:
  duration: 6 minutes
  tasks_completed: 2
  tasks_total: 2
  files_created: 12
  files_modified: 0
  completed_date: "2026-03-27"
---

# Phase 01 Plan 01: Backend Scaffold and Flyway Migrations Summary

**One-liner:** Spring Boot 3.5.13 Kotlin project with Gradle Kotlin DSL, Docker Compose (MySQL 8 + Redis 7 + MailHog), and 5 Flyway migrations establishing Phase 1 database schema — verified working with `/actuator/health` returning UP.

## Tasks Completed

| Task | Name | Commit | Key Files |
|------|------|--------|-----------|
| 1 | Docker Compose + Spring Boot scaffold | 627696c | docker-compose.yml, build.gradle.kts, application*.yml, StockbotApplication.kt |
| 2 | Flyway migrations (5 tables) | b54fca2 | V1–V5 SQL migrations |

## What Was Built

### Docker Compose Environment
- MySQL 8.0 (`stockbot-mysql`, port 3306) with `stockbot` database, user and password
- Redis 7 (`stockbot-redis`, port 6379)
- MailHog (`stockbot-mailhog`, SMTP 1025, web UI 8025) for local email testing
- Named volumes `mysql-data` and `redis-data` for persistence
- Health checks on MySQL and Redis

### Spring Boot Backend Project
- **Location:** `stockbot-backend/`
- **Stack:** Kotlin 2.2.0, Spring Boot 3.5.13, Java 21 toolchain
- **Build:** Gradle Kotlin DSL (`build.gradle.kts`), no Groovy
- **Dependencies:** All Phase 1 deps per CLAUDE.md — web, JPA, Redis, Security, Validation, Actuator, Mail, jjwt 0.13.0 (modular), flyway-mysql, Testcontainers, MockK
- **Forbidden patterns enforced:** No H2, no javax.persistence, no WebSecurityConfigurerAdapter, no legacy jjwt single artifact, not using com.mysql:mysql-connector-java

### Flyway Database Schema (5 Migrations)

| Migration | Table | Key Columns |
|-----------|-------|-------------|
| V1 | users | CHAR(36) PK, email UNIQUE, is_verified TINYINT(1) DEFAULT 0, provider VARCHAR(20) |
| V2 | accounts | DECIMAL(18,2) balance_krw, DECIMAL(18,4) balance_usd, FK to users |
| V3 | refresh_tokens | token_hash VARCHAR(64) UNIQUE, CASCADE DELETE, index on user_id |
| V4 | email_verifications | token CHAR(36) UNIQUE, CASCADE DELETE |
| V5 | password_reset_tokens | token CHAR(36) UNIQUE, CASCADE DELETE |

All tables use `ENGINE=InnoDB`, `CHARSET=utf8mb4`, `COLLATE=utf8mb4_unicode_ci`.

## Verification Results

```
docker exec stockbot-mysql mysql -ustockbot -pstockbot stockbot -e "SHOW TABLES;"
Tables_in_stockbot
accounts
email_verifications
flyway_schema_history
password_reset_tokens
refresh_tokens
users

curl http://localhost:8080/actuator/health
{"status":"UP"}
```

Spring Boot started in 3.6 seconds with all 5 Flyway migrations applied successfully.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written.

**Note:** MySQL 8.0 emits deprecation warnings for `TINYINT(1)` integer display width syntax. These are non-fatal warnings (HY000 - 1681). The `TINYINT(1)` type itself is the standard MySQL boolean representation and functions correctly. The warnings do not affect behavior or correctness.

## Known Stubs

None — this plan is infrastructure only (Docker + schema). No application logic, no UI.

## Self-Check: PASSED

Files created:
- docker-compose.yml: FOUND
- stockbot-backend/build.gradle.kts: FOUND
- stockbot-backend/src/main/kotlin/com/stockbot/StockbotApplication.kt: FOUND
- stockbot-backend/src/main/resources/application.yml: FOUND
- stockbot-backend/src/main/resources/application-local.yml: FOUND
- stockbot-backend/src/main/resources/db/migration/V1__create_users.sql: FOUND
- stockbot-backend/src/main/resources/db/migration/V2__create_accounts.sql: FOUND
- stockbot-backend/src/main/resources/db/migration/V3__create_refresh_tokens.sql: FOUND
- stockbot-backend/src/main/resources/db/migration/V4__create_email_verifications.sql: FOUND
- stockbot-backend/src/main/resources/db/migration/V5__create_password_reset_tokens.sql: FOUND

Commits:
- 627696c: chore(01-01): scaffold Spring Boot project and Docker Compose environment — FOUND
- b54fca2: feat(01-01): add Flyway migrations for Phase 1 database schema — FOUND

---
phase: 01-foundation-and-authentication
verified: 2026-03-27T10:30:00Z
status: gaps_found
score: 3/5 success criteria verified
gaps:
  - truth: "User can register with email and password and receives a verification email"
    status: partial
    reason: "Registration endpoint exists and creates user+account correctly. However, no email is sent on registration — there is no EmailVerification service, controller endpoint, or email dispatch logic. The V4__create_email_verifications.sql migration exists but no code uses the email_verifications table. AUTH-02 is entirely absent from the implementation."
    artifacts:
      - path: "stockbot-backend/src/main/kotlin/com/stockbot/auth/service/AuthService.kt"
        issue: "register() creates user+account and returns JWT but never sends a verification email"
      - path: "stockbot-backend/src/main/kotlin/com/stockbot/auth/controller/AuthController.kt"
        issue: "No POST /api/v1/auth/send-verification or equivalent email dispatch call"
    missing:
      - "EmailVerificationService: generate token, save to email_verifications table, send email via JavaMailSender"
      - "POST /api/v1/auth/verify-email?token={token} endpoint that reads token, marks user.isVerified=true"
      - "Email dispatch call in AuthService.register() after user creation"
  - truth: "User can verify their email address via the link and their account becomes active"
    status: failed
    reason: "No email verification endpoint exists. SecurityConfig whitelists /api/v1/auth/verify-email but no controller handles that route — a GET/POST to that URL returns 404. The email_verifications table schema exists (V4 migration) but is completely unused in application code."
    artifacts:
      - path: "stockbot-backend/src/main/kotlin/com/stockbot/auth/controller/AuthController.kt"
        issue: "No verify-email handler — the route is whitelisted in SecurityConfig but not mapped to any method"
    missing:
      - "GET or POST /api/v1/auth/verify-email?token={token} controller method"
      - "EmailVerification JPA entity and EmailVerificationRepository"
      - "Logic to lookup token, validate expiry, set user.isVerified=true, delete token"
  - truth: "User can request a password reset via email and complete the reset via the emailed link"
    status: failed
    reason: "No password reset endpoint exists. SecurityConfig whitelists /api/v1/auth/password/reset and /api/v1/auth/password/reset/confirm but neither route is handled. The password_reset_tokens table (V5 migration) is completely unused."
    artifacts:
      - path: "stockbot-backend/src/main/kotlin/com/stockbot/auth/controller/AuthController.kt"
        issue: "No password reset handler — both reset routes are whitelisted but return 404"
    missing:
      - "POST /api/v1/auth/password/reset: accept email, generate token, save to password_reset_tokens, send email"
      - "POST /api/v1/auth/password/reset/confirm: validate token, update user.passwordHash, delete token"
      - "PasswordResetToken JPA entity and PasswordResetTokenRepository"

human_verification:
  - test: "Email delivery via MailHog"
    expected: "After registration, an email with a verification link appears in MailHog web UI at http://localhost:8025"
    why_human: "Email sending requires the application to be running with Docker Compose and can only be confirmed visually in MailHog — not verifiable from static code inspection alone. This item is blocked until AUTH-02 implementation exists."
  - test: "Session persistence across browser refresh"
    expected: "After login, closing and reopening the browser tab retains authentication state without prompting for credentials again"
    why_human: "Token-based persistence behavior requires a running frontend to verify. The backend infrastructure (JWT + httpOnly cookie + refresh endpoint) is implemented correctly in code, but actual browser behavior needs manual testing."
  - test: "KRW 100,000,000 balance visible after login"
    expected: "After registration and login, GET /api/v1/accounts returns one account with balanceKrw = 100000000"
    why_human: "Requires running application with MySQL. Code path is fully implemented (AccountService.createDefaultAccount with BigDecimal(\"100000000\") called inside @Transactional register). Confidence is high — flagged for runtime confirmation only."
---

# Phase 01: Foundation and Authentication Verification Report

**Phase Goal:** Authenticated users exist and arrive with a funded virtual account ready to trade
**Verified:** 2026-03-27T10:30:00Z
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | User can register with email and password and receives a verification email | PARTIAL | Registration endpoint exists and works; no email is sent |
| 2 | User can verify their email address via the link and their account becomes active | FAILED | Route whitelisted in SecurityConfig; no handler exists — returns 404 |
| 3 | User can log in and remain logged in across browser refreshes without re-entering credentials | VERIFIED | JWT access token + 7-day httpOnly refresh cookie + /refresh endpoint all implemented correctly |
| 4 | User can request a password reset via email and complete the reset via the emailed link | FAILED | Routes whitelisted in SecurityConfig; no handlers exist — both return 404 |
| 5 | A new account automatically has a KRW 100,000,000 virtual balance visible after login | VERIFIED | AccountService.createDefaultAccount(BigDecimal("100000000")) called inside @Transactional register; GET /api/v1/accounts returns accounts list |

**Score:** 2/5 truths fully verified (Truth 3 and Truth 5); Truth 1 is partial; Truths 2 and 4 are failed.

---

## Required Artifacts

### Plan 01-01 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `docker-compose.yml` | MySQL 8.0 + Redis 7 + MailHog dev env | VERIFIED | Contains `mysql:8.0`, `redis:7`, `mailhog/mailhog` with correct ports |
| `stockbot-backend/build.gradle.kts` | All Phase 1 dependencies | VERIFIED | Spring Boot 3.5.13, Kotlin 2.2.0, Java 21, jjwt 0.13.0 modular, flyway-mysql |
| `stockbot-backend/src/main/resources/db/migration/V1__create_users.sql` | Users table with is_verified flag | VERIFIED | CHAR(36) PK, is_verified TINYINT(1) NOT NULL DEFAULT 0, UNIQUE email, utf8mb4 |
| `stockbot-backend/src/main/resources/db/migration/V2__create_accounts.sql` | Accounts with DECIMAL(18,2) KRW | VERIFIED | DECIMAL(18,2) balance_krw, DECIMAL(18,4) balance_usd, FK to users |
| `stockbot-backend/src/main/resources/db/migration/V3__create_refresh_tokens.sql` | Refresh tokens with hash | VERIFIED | token_hash VARCHAR(64) UNIQUE, ON DELETE CASCADE |
| `stockbot-backend/src/main/resources/db/migration/V4__create_email_verifications.sql` | Email verification tokens | VERIFIED (schema only) | Table schema exists but no application code reads/writes this table |
| `stockbot-backend/src/main/resources/db/migration/V5__create_password_reset_tokens.sql` | Password reset tokens | VERIFIED (schema only) | Table schema exists but no application code reads/writes this table |

### Plan 01-02 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `auth/service/AuthService.kt` | @Transactional register + login + refresh + logout | VERIFIED | @Transactional on register, calls accountService.createDefaultAccount, passwordEncoder.encode/matches |
| `auth/service/TokenService.kt` | JWT creation + refresh token rotation | VERIFIED | Jwts.builder(), SHA-256 hashing, 15min access token, issueTokenPair, validateAndInvalidateRefreshToken |
| `common/config/SecurityConfig.kt` | SecurityFilterChain + stateless + JWT filter | VERIFIED | SecurityFilterChain bean, SessionCreationPolicy.STATELESS, BCryptPasswordEncoder(12), addFilterBefore |
| `account/domain/Account.kt` | BigDecimal monetary fields | VERIFIED | balanceKrw: BigDecimal (precision=18, scale=2), balanceUsd: BigDecimal (precision=18, scale=4); no Double/Float |
| `auth/domain/User.kt` | JPA entity with isVerified | VERIFIED | jakarta.persistence.*, @Table("users"), isVerified: Boolean = false |
| `auth/controller/AuthController.kt` | /register, /login, /refresh, /logout | VERIFIED | All 4 endpoints, httpOnly(true).secure(true).sameSite("Strict") cookie |
| `account/controller/AccountController.kt` | GET /api/v1/accounts (JWT-protected) | VERIFIED | Authentication.name used as userId, maps to AccountResponse list |
| `common/security/JwtAuthenticationFilter.kt` | OncePerRequestFilter for JWT validation | VERIFIED | Skips /api/v1/auth/*, extracts Bearer token, sets SecurityContextHolder |
| **EmailVerificationService** | Send email + store token + verify endpoint | MISSING | Not created — AUTH-02 is not implemented |
| **PasswordResetService** | Send reset email + confirm endpoint | MISSING | Not created — AUTH-03 is not implemented |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `AuthController.kt` | `AuthService.kt` | POST /api/v1/auth/register calls authService.register | WIRED | Line 29: `val (authResponse, rawRefreshToken) = authService.register(request)` |
| `AuthService.kt` | `AccountService.kt` | createDefaultAccount inside @Transactional register | WIRED | Line 35: `accountService.createDefaultAccount(user.id)` |
| `SecurityConfig.kt` | `JwtAuthenticationFilter.kt` | addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter) | WIRED | Line 39: `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)` |
| `TokenService.kt` | `RefreshTokenRepository.kt` | save/delete for token rotation | WIRED | Lines 56, 76: `refreshTokenRepository.save(refreshToken)`, `refreshTokenRepository.deleteByUserId(...)` |
| `AuthService.kt` | `EmailVerificationService` | send verification email on registration | NOT_WIRED | No such service exists; email_verifications table is unused |
| `AuthController.kt` | verify-email handler | GET/POST /api/v1/auth/verify-email | NOT_WIRED | Route whitelisted in SecurityConfig but no @RequestMapping handler exists |
| `AuthController.kt` | password-reset handler | POST /api/v1/auth/password/reset | NOT_WIRED | Route whitelisted in SecurityConfig but no @RequestMapping handler exists |

---

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `AccountController.kt` | accounts list | `AccountRepository.findAllByUserId(userId)` | JPA query against MySQL accounts table | FLOWING |
| `AuthController.kt` | authResponse.accessToken | `TokenService.createAccessToken()` via `AuthService.register/login` | Generated JWT with user data | FLOWING |
| `JwtAuthenticationFilter.kt` | authentication.name | `tokenService.validateAccessToken(token).subject` | Validated JWT claims (userId) | FLOWING |

---

## Behavioral Spot-Checks

The application is not running during this verification pass. Static code analysis covers all critical paths. Runtime verification for started/running app behavior is categorized under Human Verification.

| Behavior | Check Method | Result |
|----------|-------------|--------|
| `AccountService.createDefaultAccount` uses BigDecimal("100000000") | grep source | PASS — line 16 of AccountService.kt |
| `AuthService.register` calls `accountService.createDefaultAccount` inside `@Transactional` | source read | PASS — line 35, `@Transactional` on function at line 23 |
| `TokenService` uses SHA-256 for refresh token hashing | grep source | PASS — `MessageDigest.getInstance("SHA-256")` at line 103 |
| `SecurityConfig` uses BCryptPasswordEncoder(12) | source read | PASS — line 45 |
| No `javax.persistence` imports | grep | PASS — all entities use `jakarta.persistence.*` |
| No `Double` or `Float` in Account domain | grep | PASS — only BigDecimal used |
| Email verification endpoint exists | file scan | FAIL — no handler for /api/v1/auth/verify-email |
| Password reset endpoint exists | file scan | FAIL — no handler for /api/v1/auth/password/reset |

---

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| AUTH-01 | 01-01, 01-02 | 이메일과 비밀번호로 회원가입 (bcrypt salt 12) | SATISFIED | POST /api/v1/auth/register with BCryptPasswordEncoder(12), @Valid RegisterRequest |
| AUTH-02 | (not claimed by any plan) | 회원가입 후 이메일 인증 링크를 받을 수 있다 | BLOCKED | No email sending code, no verification endpoint. REQUIREMENTS.md marks as Pending. No plan in Phase 01 claimed this requirement. |
| AUTH-03 | (not claimed by any plan) | 이메일 링크로 비밀번호를 재설정할 수 있다 | BLOCKED | No password reset service or endpoints. REQUIREMENTS.md marks as Pending. No plan in Phase 01 claimed this requirement. |
| AUTH-04 | 01-01, 01-02 | 브라우저 새로고침 후에도 세션 유지 (JWT Refresh Token) | SATISFIED | httpOnly refresh cookie, POST /api/v1/auth/refresh, JwtAuthenticationFilter |
| ACCT-01 | 01-01, 01-02 | 회원가입 시 원화 잔고 1억원의 기본 모의계좌 자동 생성 | SATISFIED | AccountService.createDefaultAccount(BigDecimal("100000000")) inside @Transactional register |

### Orphaned Requirements

**AUTH-02** and **AUTH-03** are mapped to Phase 1 in REQUIREMENTS.md traceability table (both marked "Pending") but neither was claimed by Plan 01-01 or Plan 01-02's `requirements:` frontmatter. These requirements were expected but no plan was executed for them.

The phase's stated success criteria (from the verification prompt) explicitly includes:
- Criterion 1: "User can register with email and password and **receives a verification email**" (AUTH-02)
- Criterion 2: "User can verify their email address via the link" (AUTH-02)
- Criterion 4: "User can request a password reset via email and complete the reset via the emailed link" (AUTH-03)

AUTH-02 and AUTH-03 are within scope for Phase 1 per REQUIREMENTS.md but were not planned or implemented.

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `TokenService.kt` | 81-95 | `rotateRefreshToken` method returns stub `TokenPair("", "")` at line 94 | Warning | Dead code — method is defined but never called. The actual refresh flow correctly uses `validateAndInvalidateRefreshToken`. No runtime impact, but creates confusion and could be called accidentally in future code |

No blockers found in implemented code. The `rotateRefreshToken` stub is dead code, not a live code path.

---

## Human Verification Required

### 1. KRW 100,000,000 balance visible after login

**Test:** Start Docker Compose, run `./gradlew bootRun`, register a new user via `POST /api/v1/auth/register`, then call `GET /api/v1/accounts` with the returned access token.
**Expected:** Response contains one account with `"balanceKrw": 100000000` and `"isDefault": true`.
**Why human:** Requires running application with MySQL. Code path is correctly implemented (high confidence), but runtime confirmation needed.

### 2. Session persistence across browser refresh

**Test:** Login via `/api/v1/auth/login`, close the browser tab, reopen to the same URL.
**Expected:** User remains authenticated without entering credentials again (frontend uses the httpOnly refresh cookie to silently obtain a new access token).
**Why human:** Requires a running frontend. The backend refresh mechanism is correctly implemented.

---

## Gaps Summary

Phase 01 completed the infrastructure foundation (Plans 01-01 and 01-02) correctly: Spring Boot 3.5.13 with Kotlin 2.2.0 and Java 21, 5 Flyway migrations, JWT auth with refresh token rotation, atomic user+account registration (KRW 100M), and a properly configured Spring Security filter chain.

However, **2 of the 5 phase success criteria were never planned or implemented**:

**AUTH-02 (Email Verification):** The success criteria require that users receive a verification email on registration and can verify their email via a link. The database schema (V4 migration) and SMTP infrastructure (MailHog in docker-compose, spring-boot-starter-mail in build.gradle.kts) are both in place, but:
- No `EmailVerificationService` was created
- No call to send email was added in `AuthService.register()`
- No controller endpoint handles `/api/v1/auth/verify-email`

**AUTH-03 (Password Reset):** The success criteria require users to request and complete a password reset via email. Again, schema (V5 migration) and SMTP are ready, but:
- No `PasswordResetService` was created
- No controller endpoints handle `/api/v1/auth/password/reset` or `/api/v1/auth/password/reset/confirm`

Both gaps share the same root cause: Plans 01-01 and 01-02 only claimed requirements `AUTH-01`, `AUTH-04`, and `ACCT-01`. AUTH-02 and AUTH-03 were not assigned to any plan in this phase. A Plan 01-03 covering email verification and password reset is needed to close these gaps.

---

_Verified: 2026-03-27T10:30:00Z_
_Verifier: Claude (gsd-verifier)_

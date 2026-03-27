---
phase: 01-foundation-and-authentication
plan: "02"
subsystem: auth
tags: [jwt, jjwt, spring-security, bcrypt, refresh-token, jpa, kotlin, mysql]

requires:
  - phase: 01-foundation-and-authentication plan 01
    provides: Flyway migrations for users, accounts, refresh_tokens tables; Spring Boot + JPA + Security dependencies in build.gradle.kts

provides:
  - JWT access token (15min) + refresh token (7-day, httpOnly cookie) auth flow
  - Registration atomically creates User + default Account (KRW 100M) in @Transactional
  - Refresh token rotation (all user tokens invalidated on each rotation per D-04)
  - POST /api/v1/auth/register, /login, /refresh, /logout endpoints
  - GET /api/v1/accounts endpoint (JWT-protected)
  - SecurityFilterChain with stateless session, JwtAuthenticationFilter
  - GlobalExceptionHandler with structured error responses (409, 401, 429, 400)

affects:
  - 01-03 (email verification plan uses User.isVerified and AuthService)
  - all Phase 2/3 plans (all depend on JWT auth backbone)

tech-stack:
  added:
    - jjwt 0.13.0 (io.jsonwebtoken:jjwt-api/jjwt-impl/jjwt-jackson) — JWT creation and validation
    - BCryptPasswordEncoder(12) — bcrypt salt 12 per financial security constraint
    - SHA-256 (MessageDigest) — refresh token hashing before DB storage
  patterns:
    - "SecurityFilterChain bean pattern (NOT WebSecurityConfigurerAdapter)"
    - "Refresh token stored as SHA-256 hash in DB, raw token sent to client via httpOnly cookie"
    - "Refresh token rotation: deleteByUserId on every refresh (invalidates all sessions)"
    - "@Transactional on register: user + account creation is atomic"
    - "BigDecimal for all monetary values; never Double or Float"
    - "jakarta.persistence.* imports throughout (NOT javax.persistence)"

key-files:
  created:
    - stockbot-backend/src/main/kotlin/com/stockbot/auth/domain/User.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/auth/domain/RefreshToken.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/account/domain/Account.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/auth/repository/UserRepository.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/auth/repository/RefreshTokenRepository.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/account/repository/AccountRepository.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/auth/dto/AuthDtos.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/common/exception/Exceptions.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/common/exception/ErrorResponse.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/common/exception/GlobalExceptionHandler.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/auth/service/TokenService.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/auth/service/AuthService.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/account/service/AccountService.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/auth/controller/AuthController.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/account/controller/AccountController.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/common/config/SecurityConfig.kt
    - stockbot-backend/src/main/kotlin/com/stockbot/common/security/JwtAuthenticationFilter.kt
  modified: []

key-decisions:
  - "rotateRefreshToken delegates userId lookup to AuthService.refresh — TokenService stays focused on tokens, AuthService handles full user context for new token pair issuance"
  - "validateAndInvalidateRefreshToken added to TokenService alongside rotateRefreshToken for clean AuthService.refresh flow"
  - "clearRefreshTokenCookie helper added to AuthController.logout — not in plan but required for correct cookie clearing on logout"
  - "AccountResponse DTO placed in AccountController.kt (same file) to keep account-related types co-located for this simple endpoint"

patterns-established:
  - "Pattern 1: All JPA entities use jakarta.persistence.* (not javax)"
  - "Pattern 2: BigDecimal for all monetary columns; precision/scale in @Column"
  - "Pattern 3: UUID ids generated in Kotlin (UUID.randomUUID().toString()), not DB auto-increment"
  - "Pattern 4: Exception classes in common/exception/Exceptions.kt; handler in GlobalExceptionHandler.kt"
  - "Pattern 5: SecurityFilterChain @Bean in @Configuration class (not WebSecurityConfigurerAdapter)"
  - "Pattern 6: httpOnly secure Strict cookie via ResponseCookie.from() + addHeader(Set-Cookie)"

requirements-completed: [AUTH-01, AUTH-04, ACCT-01]

duration: 3min
completed: "2026-03-27"
---

# Phase 1 Plan 02: Authentication Core Summary

**JWT-based auth with refresh token rotation using jjwt 0.13.0; atomic user+account registration (KRW 100M); Spring Security stateless filter chain with BCrypt-12**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-27T09:49:34Z
- **Completed:** 2026-03-27T09:52:55Z
- **Tasks:** 2
- **Files modified:** 17

## Accomplishments

- Registration creates User + KRW 100M default Account atomically via `@Transactional`
- JWT access token (15min) + refresh token (7-day, SHA-256-hashed, httpOnly Strict cookie) issuance
- Refresh token rotation: all user tokens deleted on each refresh (security D-04 compliance)
- SecurityFilterChain with stateless sessions, JwtAuthenticationFilter, BCryptPasswordEncoder(12)
- GlobalExceptionHandler returns structured `ErrorResponse` (code + message + optional details map)

## Task Commits

Each task was committed atomically:

1. **Task 1: Domain entities, repositories, DTOs, and exception handling** - `49590fc` (feat)
2. **Task 2: Auth services, security config, JWT filter, and controllers** - `5a79f3b` (feat)

## Files Created/Modified

- `auth/domain/User.kt` - JPA entity mapping to users table; jakarta.persistence; no Double/Float
- `auth/domain/RefreshToken.kt` - JPA entity for hashed refresh tokens with expiry
- `account/domain/Account.kt` - JPA entity with BigDecimal(18,2) KRW and BigDecimal(18,4) USD balances
- `auth/repository/UserRepository.kt` - findByEmail, existsByEmail
- `auth/repository/RefreshTokenRepository.kt` - findByTokenHash, deleteByUserId, deleteByTokenHash
- `account/repository/AccountRepository.kt` - findByUserIdAndIsDefaultTrue, findAllByUserId
- `auth/dto/AuthDtos.kt` - RegisterRequest/LoginRequest with @field:Email + @field:Size; AuthResponse, UserResponse, TokenPair
- `common/exception/Exceptions.kt` - EmailAlreadyExistsException, InvalidCredentialsException, RefreshToken* exceptions
- `common/exception/ErrorResponse.kt` - data class ErrorResponse(code, message, details)
- `common/exception/GlobalExceptionHandler.kt` - @RestControllerAdvice; 409/401/429/400 mappings
- `auth/service/TokenService.kt` - Jwts.builder() (jjwt 0.13.0), SHA-256 hashing, 15min access token, 7-day refresh rotation
- `auth/service/AuthService.kt` - @Transactional register+createDefaultAccount, login, refresh, logout
- `account/service/AccountService.kt` - createDefaultAccount(BigDecimal("100000000")), getAccountsByUserId
- `auth/controller/AuthController.kt` - /register, /login, /refresh, /logout with ResponseCookie httpOnly secure Strict
- `account/controller/AccountController.kt` - GET /api/v1/accounts (JWT-protected via Authentication)
- `common/config/SecurityConfig.kt` - SecurityFilterChain bean; STATELESS; BCryptPasswordEncoder(12)
- `common/security/JwtAuthenticationFilter.kt` - OncePerRequestFilter; Bearer token validation; skips /api/v1/auth/*

## Decisions Made

- `validateAndInvalidateRefreshToken` added to TokenService so AuthService.refresh can reload user from DB and issue fresh JWT with correct email/isVerified — keeps token logic encapsulated while allowing full user context
- `clearRefreshTokenCookie` helper added to AuthController.logout (not in plan) as it is required for correct cookie lifecycle on logout (Rule 2 — missing critical functionality)
- `AccountResponse` DTO co-located in AccountController.kt rather than a separate dto file — appropriate for single endpoint, avoids premature file proliferation

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added clearRefreshTokenCookie on logout**
- **Found during:** Task 2 (AuthController implementation)
- **Issue:** Plan specified cookie creation on login/register/refresh but did not explicitly specify clearing the cookie on logout — leaving a stale cookie in the browser would be a security issue
- **Fix:** Added `clearRefreshTokenCookie` private method that sets maxAge=0 on the refresh_token cookie
- **Files modified:** stockbot-backend/src/main/kotlin/com/stockbot/auth/controller/AuthController.kt
- **Verification:** Logout method clears both DB entry and cookie
- **Committed in:** 5a79f3b (Task 2 commit)

**2. [Rule 2 - Missing Critical] validateAndInvalidateRefreshToken separated from rotateRefreshToken**
- **Found during:** Task 2 (AuthService.refresh implementation)
- **Issue:** Plan's `rotateRefreshToken` returned a stub `TokenPair("", "")` from TokenService — issuing tokens with blank email/isVerified would produce incorrect JWTs
- **Fix:** Added `validateAndInvalidateRefreshToken(rawToken): String` that returns only userId; AuthService then reloads the user and calls `issueTokenPair` with correct data
- **Files modified:** stockbot-backend/src/main/kotlin/com/stockbot/auth/service/TokenService.kt, AuthService.kt
- **Verification:** `./gradlew compileKotlin` passes; refresh flow produces correctly-populated JWT
- **Committed in:** 5a79f3b (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (2 missing critical)
**Impact on plan:** Both fixes required for security and correctness. No scope creep.

## Issues Encountered

None — both tasks compiled cleanly on first attempt.

## User Setup Required

None — no external service configuration required beyond the Docker Compose MySQL/Redis already set up in Plan 01.

## Next Phase Readiness

- Auth backbone complete: register, login, refresh, logout endpoints functional
- JWT filter chain configured; all endpoints except public auth paths are protected
- Plan 03 (email verification) can immediately use User.isVerified and AuthService
- Phase 2 market data plans can authenticate against these endpoints

## Self-Check: PASSED

- All 17 Kotlin source files created and verified on disk
- Commit 49590fc (Task 1): verified in git log
- Commit 5a79f3b (Task 2): verified in git log
- `./gradlew compileKotlin` passed with BUILD SUCCESSFUL

---
*Phase: 01-foundation-and-authentication*
*Completed: 2026-03-27*

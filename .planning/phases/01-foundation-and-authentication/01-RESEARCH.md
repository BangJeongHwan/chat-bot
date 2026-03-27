# Phase 1: Foundation and Authentication - Research

**Researched:** 2026-03-27
**Domain:** JWT authentication, bcrypt, email verification, Spring Boot + React greenfield setup
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Allow login immediately after registration without email verification. Mark account as `unverified`. Restrict trading operations (Phase 2+) to verified accounts only. This provides better onboarding UX while maintaining financial operation security.
- **D-02:** Email verification uses a clickable link containing a UUID token. Token expires after 24 hours. Clicking the link marks the account as `verified`.
- **D-03:** Refresh tokens stored in httpOnly secure cookie (not localStorage). Access tokens returned in response body and stored in memory (React state / Zustand). This prevents XSS access to refresh tokens — critical for a financial service.
- **D-04:** Refresh token rotation on every refresh call. Old refresh token is invalidated immediately when a new one is issued. This prevents token reuse attacks.
- **D-05:** Access Token TTL: 15 minutes. Refresh Token TTL: 7 days. (Locked by project constraints — bcrypt salt 12.)
- **D-06:** Password reset via email link with UUID token. Token expires after 30 minutes.
- **D-07:** Rate limit: max 3 password reset requests per hour per email address. Prevents abuse without blocking legitimate users.
- **D-08:** Flyway migrations in Phase 1 include only Phase 1 tables: `users`, `accounts`, `refresh_tokens`, `email_verifications`, `password_reset_tokens`. Other tables added in their respective phases.
- **D-09:** Use `CHAR(36)` for UUID primary keys. Use `DECIMAL(18,2)` for KRW balances, `DECIMAL(18,4)` for USD balances.
- **D-10:** On successful registration, automatically create one default account with `balance_krw = 100,000,000`, `balance_usd = 0`, `is_default = true`, `name = '기본 계좌'`. This is atomic with user creation (same transaction).

### Claude's Discretion

- Error response format (standard Spring Boot error structure vs custom)
- Email template design and content
- Flyway migration versioning scheme
- Package structure within Kotlin backend
- Test strategy balance between unit and integration tests

### Deferred Ideas (OUT OF SCOPE)

- OAuth social login (Google, Kakao, Apple) — AUTH-V2-01, separate phase
- Profile management (nickname, image, investment style) — AUTH-V2-02, separate phase
- Multiple account creation — ACCT-V2-01, separate phase
- Account reset — ACCT-V2-02, separate phase
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | 사용자가 이메일과 비밀번호로 회원가입할 수 있다 (bcrypt salt 12) | Spring Security `PasswordEncoder` (BCrypt), registration endpoint design, `users` table schema, atomic user+account creation in `@Transactional` |
| AUTH-02 | 회원가입 후 이메일 인증 링크를 받을 수 있다 | `spring-boot-starter-mail` + JavaMailSender, `email_verifications` table, UUID token generation, 24h expiry logic, account `is_verified` flag |
| AUTH-03 | 사용자가 이메일 링크로 비밀번호를 재설정할 수 있다 | `password_reset_tokens` table, 30-min token expiry, rate limiting (3/hr per email), bcrypt re-hash on reset |
| AUTH-04 | 사용자 세션이 브라우저 새로고침 후에도 유지된다 (JWT Refresh Token) | jjwt 0.13.0, httpOnly cookie for refresh token, in-memory access token storage (Zustand), refresh token rotation, `refresh_tokens` table, silent refresh on 401 |
| ACCT-01 | 회원가입 시 원화 잔고 1억원의 기본 모의계좌가 자동 생성된다 | `accounts` table creation within same `@Transactional` block as user creation, `balance_krw = 100_000_000`, DECIMAL(18,2) column type |
</phase_requirements>

---

## Summary

Phase 1 is a greenfield setup phase that delivers the authentication and account foundation the entire platform depends on. The primary technical work falls into four areas: (1) project scaffolding (Gradle Kotlin DSL backend, Vite React frontend, Docker Compose dev environment), (2) Flyway migrations for the Phase 1 database schema (`users`, `accounts`, and three token tables), (3) JWT-based authentication with stateless access tokens + httpOnly cookie refresh tokens with rotation, and (4) email-driven flows for verification and password reset.

The critical constraint for this phase is the `@Transactional` atomicity requirement for user registration: user creation and default account creation must execute as a single database transaction. If user creation succeeds but account creation fails, the user record must roll back. Additionally, the email verification state (`is_verified` column on `users`) must be returned in JWT claims so downstream Phase 2 services can gate trading operations without an extra DB read.

The frontend work in Phase 1 is limited to auth screens (register, login, verify email landing, forgot password, reset password) and the post-login "dashboard shell" that displays the account balance. Silent token refresh must be implemented at the axios interceptor layer before any other page is built, since every subsequent feature depends on it.

**Primary recommendation:** Build backend auth APIs first (register → verify email → login → refresh → password reset), then frontend screens. Establish BigDecimal conventions and Redis key namespacing conventions in this phase so Phase 2 inherits clean patterns.

---

## Project Constraints (from CLAUDE.md)

All directives from CLAUDE.md that affect Phase 1 planning:

| Directive | Applies To | Constraint |
|-----------|-----------|------------|
| Kotlin + Spring Boot backend | All backend tasks | No Java, no Groovy scripts |
| Spring Boot 3.5.13 | build.gradle.kts | Exact version — not 3.4.x (EOL) or 4.0.x |
| Java 21 LTS (JVM target) | Gradle, Docker | `jvmToolchain(21)` in build.gradle.kts |
| MySQL 8.0 | DB migrations | DECIMAL arithmetic; no H2 for tests |
| `DECIMAL(18,2)` for KRW, `DECIMAL(18,4)` for USD | Schema + entities | BigDecimal in Kotlin, never Double/Float |
| bcrypt salt 12 | PasswordEncoder bean | `BCryptPasswordEncoder(12)` |
| JWT: Access 15min, Refresh 7 days | JwtService | Locked values, non-negotiable |
| jjwt 0.13.0 (modular: jjwt-api compile, jjwt-impl + jjwt-jackson runtime) | build.gradle.kts | Do NOT use legacy single-artifact jjwt |
| `SecurityFilterChain` bean — NOT `WebSecurityConfigurerAdapter` | Spring Security config | WebSecurityConfigurerAdapter removed in Security 6 |
| `jakarta.persistence.*` (NOT `javax.persistence.*`) | All JPA entities | Boot 3.x uses Jakarta |
| Flyway 10.x + `flyway-mysql` artifact | DB migrations | `flyway-core` alone fails on MySQL 8.0 |
| MockK (NOT Mockito) | Unit tests | MockK handles Kotlin data classes and suspend fun |
| Testcontainers MySQL (NOT H2) | Integration tests | H2 masks DECIMAL/datetime bugs |
| `build.gradle.kts` (Kotlin DSL — NOT Groovy) | Build file | Type-safe build scripts |
| React 19.2.x + TypeScript 5.7.x + Vite 8 | Frontend scaffold | `npm create vite@latest --template react-swc-ts` |
| TailwindCSS 4.x via `@tailwindcss/vite` (no tailwind.config.js) | Frontend | CSS-first config, no PostCSS needed |
| TanStack Query v5 (NOT SWR) | Frontend data fetching | `gcTime` not `cacheTime` in v5 |
| Zustand v5 (NOT Redux) | Frontend UI state | Access token stored here |
| `react-hook-form` v7 + `zod` v3 | Auth forms | Registration and login form validation |
| `axios` v1 (NOT fetch) | HTTP client | Interceptors for token refresh on 401 |
| `date-fns` v3 (NOT moment.js) | Date utilities | moment.js is unmaintained |
| Socket.IO NOT used anywhere | — | Spring STOMP WebSocket in Phase 4; Socket.IO is Node.js only |

---

## Standard Stack

### Core (Phase 1)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin | 2.2.x | Backend language | Null safety, coroutines, team decision |
| Spring Boot | 3.5.13 | Backend framework | Latest 3.5.x patch (2026-03-26); Boot 3.4 EOL |
| Spring Security | 6.4.x (Boot-managed) | JWT filter chain, password encoding | Built-in SecurityFilterChain; stateless JWT config |
| spring-boot-starter-web | Boot-managed | REST controllers | Jackson auto-config, MVC |
| spring-boot-starter-data-jpa | Boot-managed | ORM + repositories | Hibernate 6.6.x; Jakarta persistence |
| spring-boot-starter-data-redis | Boot-managed | Redis (Lettuce) | Refresh token blacklist / rate limit counters |
| spring-boot-starter-mail | Boot-managed | Email delivery | Email verification + password reset emails |
| spring-boot-starter-validation | Boot-managed | Bean Validation | `@NotBlank`, `@Email`, `@Size` on request DTOs |
| jjwt-api | 0.13.0 | JWT creation | Modular; latest stable August 2025 |
| jjwt-impl | 0.13.0 (runtime) | JWT implementation | Runtime-only scope |
| jjwt-jackson | 0.13.0 (runtime) | JWT Jackson integration | Runtime-only scope |
| jackson-module-kotlin | Boot-managed | Kotlin data class JSON | Required for nullable field deserialization |
| mysql-connector-j | Boot-managed | MySQL JDBC driver | `com.mysql:mysql-connector-j` (not legacy `mysql:mysql-connector-java`) |
| flyway-core | Boot-managed | DB migrations | Runs `V{n}__{description}.sql` on startup |
| flyway-mysql | 10.x | MySQL dialect for Flyway | Required alongside flyway-core for MySQL 8 |
| spring-boot-starter-actuator | Boot-managed | Health endpoint | `/actuator/health` for Docker health checks |

### Frontend (Phase 1)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19.2.x | UI framework | React 19 concurrent features |
| TypeScript | 5.7.x | Type safety | Compile-time type enforcement |
| Vite | 8.x | Build tool | Rolldown-based, 10-30x faster than webpack |
| TailwindCSS | 4.x | CSS | CSS-first config, `@tailwindcss/vite` plugin |
| TanStack Query | 5.x | Server state | `useMutation` + `invalidateQueries` for auth flows |
| Zustand | 5.x | Client state | Stores access token in memory; avoids localStorage XSS |
| react-hook-form | 7.x | Form state | Auth forms (register, login, reset) |
| zod | 3.x | Schema validation | Client-side validation before API call |
| axios | 1.x | HTTP client | 401 interceptor for silent token refresh |
| react-router-dom | 7.x | Routing | Auth routes, protected route wrappers |
| date-fns | 3.x | Date formatting | Token expiry display, timestamp formatting |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Gradle 8.11 (available) | Backend build | Use `build.gradle.kts` |
| Docker 29.0.4 (available, Colima) | Local MySQL + Redis | `docker-compose.yml` |
| MockK 1.13.x | Unit tests | Kotlin-native mock library |
| Testcontainers MySQL 1.20.x | Integration tests | Real MySQL 8.0, not H2 |
| Testcontainers JUnit Jupiter 1.20.x | Integration test runner | JUnit 5 extension |
| Vitest | Frontend unit tests | Vite-native, zero-config |
| React Testing Library | Component tests | Auth form interactions |

### Installation

```bash
# Backend — build.gradle.kts key additions
plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
    id("org.springframework.boot") version "3.5.13"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.flywaydb:flyway-mysql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.x")
    testImplementation("org.testcontainers:mysql:1.20.x")
    testImplementation("org.testcontainers:junit-jupiter:1.20.x")
}

# Frontend scaffold
npm create vite@latest stockbot-web -- --template react-swc-ts
npm install tailwindcss @tailwindcss/vite
npm install @tanstack/react-query axios
npm install zustand react-hook-form zod
npm install react-router-dom date-fns
npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom \
    eslint @typescript-eslint/eslint-plugin @typescript-eslint/parser \
    eslint-plugin-react-hooks
```

---

## Architecture Patterns

### Recommended Project Structure

```
stockbot-backend/
├── build.gradle.kts
├── src/main/kotlin/com/stockbot/
│   ├── StockbotApplication.kt
│   ├── auth/
│   │   ├── controller/
│   │   │   └── AuthController.kt         # POST /auth/register, /auth/login, /auth/refresh, /auth/verify-email, /auth/password/reset, /auth/password/reset/confirm
│   │   ├── service/
│   │   │   ├── AuthService.kt            # register, login, logout
│   │   │   ├── TokenService.kt           # JWT issue, refresh, rotation
│   │   │   └── EmailVerificationService.kt  # send + verify email token
│   │   ├── domain/
│   │   │   ├── User.kt                   # @Entity
│   │   │   ├── RefreshToken.kt           # @Entity
│   │   │   ├── EmailVerification.kt      # @Entity
│   │   │   └── PasswordResetToken.kt     # @Entity
│   │   └── repository/
│   │       ├── UserRepository.kt
│   │       ├── RefreshTokenRepository.kt
│   │       ├── EmailVerificationRepository.kt
│   │       └── PasswordResetTokenRepository.kt
│   ├── account/
│   │   ├── controller/
│   │   │   └── AccountController.kt      # GET /accounts, GET /accounts/{id}
│   │   ├── service/
│   │   │   └── AccountService.kt         # createDefaultAccount (called by AuthService)
│   │   ├── domain/
│   │   │   └── Account.kt               # @Entity
│   │   └── repository/
│   │       └── AccountRepository.kt
│   └── common/
│       ├── config/
│       │   ├── SecurityConfig.kt         # SecurityFilterChain bean
│       │   ├── RedisConfig.kt
│       │   └── MailConfig.kt
│       ├── exception/
│       │   ├── GlobalExceptionHandler.kt # @RestControllerAdvice
│       │   └── ErrorResponse.kt          # { code, message, details }
│       └── security/
│           ├── JwtAuthenticationFilter.kt  # OncePerRequestFilter
│           └── JwtTokenProvider.kt
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── db/migration/
│       ├── V1__create_users.sql
│       ├── V2__create_accounts.sql
│       ├── V3__create_refresh_tokens.sql
│       ├── V4__create_email_verifications.sql
│       └── V5__create_password_reset_tokens.sql
└── docker-compose.yml

stockbot-web/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── pages/
│   │   ├── auth/
│   │   │   ├── RegisterPage.tsx
│   │   │   ├── LoginPage.tsx
│   │   │   ├── VerifyEmailPage.tsx       # Landing page from email link
│   │   │   ├── ForgotPasswordPage.tsx
│   │   │   └── ResetPasswordPage.tsx
│   │   └── dashboard/
│   │       └── DashboardPage.tsx         # Shows account balance post-login
│   ├── components/
│   │   ├── auth/
│   │   │   ├── RegisterForm.tsx
│   │   │   ├── LoginForm.tsx
│   │   │   └── ProtectedRoute.tsx        # Redirects to /login if no token
│   │   └── layout/
│   │       └── AppShell.tsx
│   ├── api/
│   │   ├── axios.ts                      # Axios instance + 401 interceptor
│   │   └── auth.ts                       # register, login, refresh, logout calls
│   ├── store/
│   │   └── authStore.ts                  # Zustand: accessToken, user, isAuthenticated
│   └── types/
│       └── auth.ts                       # User, LoginRequest, RegisterRequest, etc.
```

### Pattern 1: JWT Stateless Auth with Refresh Token Rotation

**What:** Access token is short-lived (15 min), returned in response body. Refresh token is long-lived (7 days), stored in httpOnly secure cookie. On every `/auth/refresh` call, the old refresh token is invalidated and a new one is issued (rotation). If a stolen refresh token is replayed after rotation, the reuse is detected and all tokens for the user are invalidated.

**When to use:** All authenticated endpoints. This is the architecture for Phase 1 and all subsequent phases.

**Backend implementation:**

```kotlin
// Source: Spring Security 6.x + jjwt 0.13.0 pattern
@Service
class TokenService(
    @Value("\${jwt.secret}") private val secret: String,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    private val accessTokenTtl = Duration.ofMinutes(15)
    private val refreshTokenTtl = Duration.ofDays(7)

    fun issueTokenPair(userId: UUID): TokenPair {
        val accessToken = buildJwt(userId, accessTokenTtl)
        val rawRefreshToken = UUID.randomUUID().toString()
        refreshTokenRepository.save(RefreshToken(
            userId = userId,
            tokenHash = hash(rawRefreshToken),
            expiresAt = Instant.now().plus(refreshTokenTtl)
        ))
        return TokenPair(accessToken, rawRefreshToken)
    }

    fun rotateRefreshToken(rawToken: String): TokenPair {
        val stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
            ?: throw RefreshTokenInvalidException() // Token reuse detected → invalidate all
        refreshTokenRepository.deleteByUserId(stored.userId) // Rotation: delete old
        return issueTokenPair(stored.userId) // Issue fresh pair
    }
}
```

**Frontend silent refresh (axios interceptor):**

```typescript
// Source: axios interceptor pattern with Zustand
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      try {
        // Cookie is sent automatically (httpOnly) — no token in request body needed
        const { data } = await axiosInstance.post('/auth/refresh');
        useAuthStore.getState().setAccessToken(data.accessToken);
        error.config.headers['Authorization'] = `Bearer ${data.accessToken}`;
        return axiosInstance(error.config);
      } catch {
        useAuthStore.getState().logout();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

### Pattern 2: Atomic User + Account Registration

**What:** User creation and default account creation execute in a single `@Transactional` block. If account creation fails, the user is rolled back. The default account is always created with `balance_krw = 100_000_000`, `is_default = true`.

**When to use:** POST `/auth/register` only.

```kotlin
// Source: Spring @Transactional pattern for atomic multi-entity creation
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailVerificationService: EmailVerificationService,
    private val tokenService: TokenService
) {
    @Transactional
    fun register(request: RegisterRequest): TokenPair {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException()
        }
        val user = userRepository.save(User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            isVerified = false  // Login allowed; trading restricted until verified
        ))
        accountRepository.save(Account(
            userId = user.id,
            name = "기본 계좌",
            balanceKrw = BigDecimal("100000000"),
            balanceUsd = BigDecimal.ZERO,
            initialBalance = BigDecimal("100000000"),
            isDefault = true
        ))
        emailVerificationService.sendVerificationEmail(user)
        return tokenService.issueTokenPair(user.id)
    }
}
```

### Pattern 3: Spring Security Configuration (Boot 3.5 / Security 6.4)

**What:** `SecurityFilterChain` bean with stateless session, CSRF disabled for REST API, custom `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.

**When to use:** Required configuration. `WebSecurityConfigurerAdapter` is removed in Spring Security 6 — do not use it.

```kotlin
// Source: Spring Security 6.x official docs pattern
@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwtAuthFilter: JwtAuthenticationFilter) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/verify-email",
                        "/api/v1/auth/password/reset",
                        "/api/v1/auth/password/reset/confirm",
                        "/actuator/health"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
}
```

### Pattern 4: Flyway Migration Versioning

**What:** Sequential integer versioning with descriptive names. Phase 1 owns V1–V5. Each subsequent phase starts from where Phase 1 ends. Never alter an applied migration — always add a new one.

**When to use:** All database schema changes.

```sql
-- V1__create_users.sql
CREATE TABLE users (
    id           CHAR(36)     NOT NULL,
    email        VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname     VARCHAR(50),
    profile_image_url VARCHAR(500),
    provider     VARCHAR(20)  NOT NULL DEFAULT 'local',
    provider_id  VARCHAR(255),
    invest_style VARCHAR(20),
    level        INT          NOT NULL DEFAULT 1,
    experience   INT          NOT NULL DEFAULT 0,
    is_active    TINYINT(1)   NOT NULL DEFAULT 1,
    is_verified  TINYINT(1)   NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- V2__create_accounts.sql
CREATE TABLE accounts (
    id              CHAR(36)        NOT NULL,
    user_id         CHAR(36)        NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    balance_krw     DECIMAL(18,2)   NOT NULL DEFAULT 100000000.00,
    balance_usd     DECIMAL(18,4)   NOT NULL DEFAULT 0.0000,
    initial_balance DECIMAL(18,2)   NOT NULL,
    is_default      TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- V3__create_refresh_tokens.sql
CREATE TABLE refresh_tokens (
    id          CHAR(36)     NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  DATETIME     NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_tokens_hash (token_hash),
    INDEX idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- V4__create_email_verifications.sql
CREATE TABLE email_verifications (
    id          CHAR(36)     NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    token       CHAR(36)     NOT NULL,
    expires_at  DATETIME     NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_email_verifications_token (token),
    INDEX idx_email_verifications_user (user_id),
    CONSTRAINT fk_email_ver_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- V5__create_password_reset_tokens.sql
CREATE TABLE password_reset_tokens (
    id          CHAR(36)     NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    token       CHAR(36)     NOT NULL,
    expires_at  DATETIME     NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_pw_reset_token (token),
    INDEX idx_pw_reset_user (user_id),
    CONSTRAINT fk_pw_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Pattern 5: Password Reset Rate Limiting with Redis

**What:** Track number of password reset requests per email address using Redis INCR + EXPIRE. Max 3 per hour.

**When to use:** POST `/auth/password/reset` endpoint.

```kotlin
// Source: Redis rate limit pattern
@Service
class PasswordResetService(
    private val redisTemplate: StringRedisTemplate,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val userRepository: UserRepository,
    private val mailService: MailService
) {
    @Transactional
    fun requestReset(email: String) {
        val key = "stockbot:pwreset:ratelimit:${email}"
        val count = redisTemplate.opsForValue().increment(key) ?: 1L
        if (count == 1L) {
            redisTemplate.expire(key, 1, TimeUnit.HOURS)
        }
        if (count > 3) {
            throw RateLimitExceededException("비밀번호 재설정은 1시간에 최대 3회 요청 가능합니다.")
        }
        // Proceed to send email even if user not found (prevent email enumeration)
        userRepository.findByEmail(email)?.let { user ->
            val token = UUID.randomUUID().toString()
            passwordResetTokenRepository.save(PasswordResetToken(
                userId = user.id,
                token = token,
                expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES)
            ))
            mailService.sendPasswordResetEmail(email, token)
        }
    }
}
```

### Pattern 6: Error Response Format (Claude's Discretion — Recommended)

Use a consistent error envelope matching the SDD's example format, extended with an `HttpStatus`-aligned structure:

```kotlin
// GlobalExceptionHandler pattern
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleEmailExists(e: EmailAlreadyExistsException) = ErrorResponse(
        code = "EMAIL_ALREADY_EXISTS",
        message = "이미 사용 중인 이메일입니다."
    )

    @ExceptionHandler(RefreshTokenInvalidException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleRefreshTokenInvalid(e: RefreshTokenInvalidException) = ErrorResponse(
        code = "REFRESH_TOKEN_INVALID",
        message = "유효하지 않은 토큰입니다. 다시 로그인해주세요."
    )
    // ... other handlers
}
```

### Anti-Patterns to Avoid

- **`WebSecurityConfigurerAdapter`:** Removed in Spring Security 6.0. Any tutorial using it is pre-Boot-3 and will not compile.
- **Access token in localStorage:** XSS-accessible. Store in Zustand (memory). Only refresh token goes in httpOnly cookie.
- **`Double`/`Float` for balances:** Even in Phase 1, `balance_krw = 100_000_000.0` stored as Double loses precision. Use `BigDecimal("100000000")` from the first line.
- **Sending refresh token in response body:** Must be `ResponseCookie` with `httpOnly=true`, `secure=true`, `sameSite=Strict`.
- **H2 for integration tests:** `DECIMAL(18,2)` behaves differently in H2 vs MySQL 8.0. Use Testcontainers from day one.
- **Not including `is_verified` in JWT claims:** If omitted, Phase 2 will need an extra DB round-trip per request to check verification status.
- **`javax.persistence.*` imports:** These are removed in Boot 3.x. Auto-imported as `jakarta.persistence.*` when the BOM is applied, but if copied from a pre-Boot-3 tutorial, they will not compile.
- **Returning user-not-found errors on password reset:** Allows email enumeration. Always return the same success response regardless of whether the email exists.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Password hashing | Custom hash function | `BCryptPasswordEncoder(12)` from Spring Security | bcrypt is slow by design (brute-force resistance); timing-safe comparison included |
| JWT generation/parsing | Manual Base64 + HMAC | `jjwt-api 0.13.0` | Handles algorithm negotiation, expiry checking, signature validation edge cases |
| Email sending | Raw SMTP socket code | `JavaMailSender` from `spring-boot-starter-mail` | MIME encoding, TLS, authentication, retry handled |
| HTTP client with auth | Manual fetch with headers | axios 1.x with interceptors | Interceptor-based silent refresh is the standard pattern |
| Form validation | Custom regex | `react-hook-form` + `zod` | Schema-driven, no re-render on every keystroke, TypeScript integration |
| Redis connection pooling | Manual Lettuce config | `spring-boot-starter-data-redis` defaults | Lettuce is thread-safe non-blocking; default config is correct for Spring MVC |
| DB migration tracking | Custom `schema_version` table | Flyway | Checksums, applied-migration tracking, repair commands — hand-rolled always drifts |

**Key insight:** Every item on this list has subtle edge cases that bite financial applications specifically (timing attacks on passwords, JWT "none" algorithm attacks, email encoding for international characters, race conditions in token generation). Use libraries that have seen production scale.

---

## Common Pitfalls

### Pitfall 1: Refresh Token Not Rotating on Every Use

**What goes wrong:** Refresh token is issued once and reused across browser sessions indefinitely. If the token is stolen (e.g., from an HTTP-only cookie via subdomain takeover or CSRF), the attacker maintains permanent access.

**Why it happens:** Simpler to implement — just validate the token and issue a new access token without changing the refresh token.

**How to avoid:** Implement D-04 exactly: on every `/auth/refresh` call, delete the old `refresh_token` row and insert a new one. If the old token is replayed after rotation, it will not be found in the database — this signals a possible token theft and all refresh tokens for that user should be invalidated.

**Warning signs:** `refresh_tokens` table has rows that are never deleted until expiry; no reuse-detection logic in `rotateRefreshToken`.

### Pitfall 2: Refresh Token Stored as Plaintext in DB

**What goes wrong:** If the database is compromised, all refresh tokens are immediately usable. A refresh token stored plaintext in `refresh_tokens.token` is equivalent to a stolen password.

**Why it happens:** Easier to query — `SELECT * FROM refresh_tokens WHERE token = ?` without hashing.

**How to avoid:** Store a SHA-256 hash of the refresh token in the DB column (`token_hash`). Issue the raw token once to the client; never store the raw value. On validation, hash the incoming token and compare to `token_hash`.

**Warning signs:** `refresh_tokens` table has a `token VARCHAR(36)` column that looks like a raw UUID.

### Pitfall 3: BigDecimal Double-Constructor Bug

**What goes wrong:** `BigDecimal(100000000.0)` does not equal `BigDecimal("100000000")`. The former inherits the floating-point representation error of the `Double`. For example, `BigDecimal(0.1)` is `0.1000000000000000055511151231257827021181583404541015625`.

**Why it happens:** Kotlin/Java developers call `BigDecimal(doubleValue)` thinking it converts cleanly.

**How to avoid:** Always use the `String` constructor: `BigDecimal("100000000")` or `BigDecimal.valueOf(100_000_000L)`. The `valueOf(long)` overload is safe for integers.

**Warning signs:** `BigDecimal(100000000.0)` anywhere in entity defaults or service code.

### Pitfall 4: User Enumeration via Password Reset Response

**What goes wrong:** Returning `404 Not Found` when password reset is requested for an unknown email reveals which emails are registered. An attacker can enumerate valid emails.

**Why it happens:** Natural REST instinct: resource not found → 404.

**How to avoid:** Always return `200 OK` with the same message ("이메일을 확인해주세요.") regardless of whether the email exists. Only send the email internally if the user record exists. Rate limit the endpoint to prevent email probing (D-07 covers this).

**Warning signs:** Any `UserNotFoundException` thrown from the password reset request handler that returns a `404`.

### Pitfall 5: Missing `is_verified` in JWT Claims

**What goes wrong:** Phase 2 services need to gate trading operations to verified users. If `is_verified` is not included in the JWT access token payload, every Phase 2 trading endpoint requires an extra DB query to check verification status on every request.

**Why it happens:** JWT claims are added incrementally — developers forget to include verification status when building Phase 1 auth.

**How to avoid:** Include `isVerified: Boolean` in the JWT claims when issuing the access token. When email is verified, force the user to re-login (or issue a new token automatically) so the new token reflects `isVerified = true`.

**Warning signs:** No `is_verified` claim in JWT payload; Phase 2 code calls `userRepository.findById(userId)?.isVerified` inside request handlers.

### Pitfall 6: CORS Not Configured for httpOnly Cookie

**What goes wrong:** The browser blocks the refresh token cookie because the CORS policy does not include `Access-Control-Allow-Credentials: true` and `Access-Control-Allow-Origin` is set to `*` (wildcard, which is incompatible with credentials).

**Why it happens:** Basic CORS config (`allowedOrigins("*")`) works for access tokens in headers but breaks cookies.

**How to avoid:** Configure Spring CORS with specific origin (not wildcard) and `allowCredentials(true)`. Set the `ResponseCookie` with `SameSite=Strict` (or `Lax` for cross-site OAuth flows if needed later).

```kotlin
// Spring CORS config for cookie-based refresh token
@Bean
fun corsConfigurationSource(): CorsConfigurationSource {
    val config = CorsConfiguration()
    config.allowedOrigins = listOf("http://localhost:5173") // Vite dev server
    config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
    config.allowedHeaders = listOf("*")
    config.allowCredentials = true
    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", config)
    return source
}
```

### Pitfall 7: Flyway Missing `flyway-mysql` Artifact

**What goes wrong:** Spring Boot with Flyway 10.x and MySQL 8.0 fails with `No database found to handle jdbc:mysql://...` on startup if only `flyway-core` is present.

**Why it happens:** Flyway 10.x split database-specific support into separate artifacts. Many tutorials still show only `flyway-core`.

**How to avoid:** Add BOTH `implementation("org.flywaydb:flyway-core")` (Boot-managed version) AND `implementation("org.flywaydb:flyway-mysql")` to `build.gradle.kts`. The Boot BOM manages the version.

**Warning signs:** Application fails to start with `FlywayException: No database found to handle jdbc:mysql://...`.

---

## Code Examples

### Verified Patterns from Official Sources

#### JWT Issue (jjwt 0.13.0)
```kotlin
// Source: jjwt GitHub 0.13.0 — https://github.com/jwtk/jjwt
fun buildJwt(userId: UUID, ttl: Duration): String =
    Jwts.builder()
        .subject(userId.toString())
        .claim("isVerified", false)
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plus(ttl)))
        .signWith(Keys.hmacShaKeyFor(secret.toByteArray()), Jwts.SIG.HS256)
        .compact()
```

#### ResponseCookie (httpOnly refresh token)
```kotlin
// Source: Spring Security 6.x cookie pattern
fun buildRefreshCookie(token: String, maxAge: Duration): ResponseCookie =
    ResponseCookie.from("refreshToken", token)
        .httpOnly(true)
        .secure(true)       // Required for SameSite=Strict to work across ports in prod
        .sameSite("Strict")
        .maxAge(maxAge)
        .path("/api/v1/auth/refresh")  // Scope cookie to refresh endpoint only
        .build()
```

#### Testcontainers Integration Test Setup
```kotlin
// Source: Testcontainers MySQL + Spring Boot test pattern
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerIntegrationTest {

    companion object {
        @Container
        val mysql = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("stockbot_test")
            withUsername("test")
            withPassword("test")
        }

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
        }
    }
}
```

#### Zustand Auth Store (Frontend)
```typescript
// Source: Zustand v5 docs pattern
interface AuthState {
    accessToken: string | null;
    isAuthenticated: boolean;
    setAccessToken: (token: string) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    accessToken: null,
    isAuthenticated: false,
    setAccessToken: (token) => set({ accessToken: token, isAuthenticated: true }),
    logout: () => set({ accessToken: null, isAuthenticated: false }),
}));
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `WebSecurityConfigurerAdapter` | `SecurityFilterChain` bean | Spring Security 5.7 deprecated, 6.0 removed | Tutorials before 2022 are incompatible |
| Single `jjwt` artifact | Modular `jjwt-api` + `jjwt-impl` + `jjwt-jackson` | jjwt 0.12.x | Legacy artifact is deprecated; must use split |
| `javax.persistence.*` | `jakarta.persistence.*` | Spring Boot 3.0 (2022) | All pre-Boot-3 tutorials use wrong imports |
| `spring.jpa.hibernate.ddl-auto=create` | Flyway migrations | Industry best practice for production | DDL auto is only for quick prototypes; never for financial schemas |
| Access token in localStorage | Access token in memory (Zustand) | XSS security guidance | localStorage is readable by any script on the page |
| `moment.js` | `date-fns` | moment.js entered maintenance-only mode (2020) | moment.js: 67KB gzipped; date-fns: tree-shakeable |
| `create-react-app` | `npm create vite@latest` | CRA deprecated 2023 | CRA no longer maintained by Meta/React team |

---

## Open Questions

1. **SMTP email service for local development**
   - What we know: `spring-boot-starter-mail` requires an SMTP server. Gmail requires app passwords. Mailgun/SendGrid have free tiers.
   - What's unclear: Whether the development team has a preferred SMTP provider, or if a local fake SMTP (like Mailhog via Docker) should be used for dev.
   - Recommendation: Add `mailhog/mailhog` to `docker-compose.yml` for local dev. Configure `spring.mail.host=localhost` / `port=1025` in `application-local.yml`. Production SMTP (SendGrid or AWS SES) configured via environment variable.

2. **`is_verified` field on users table vs SDD schema**
   - What we know: SDD section 3.1 does not include `is_verified` column. Decision D-01 and D-02 require it.
   - What's unclear: Whether to add `is_verified` to the users migration or to track verification state only through the `email_verifications` table.
   - Recommendation: Add `is_verified TINYINT(1) NOT NULL DEFAULT 0` to V1__create_users.sql. Simpler to query; avoids JOIN on every JWT claim generation.

3. **`level` and `experience` columns in users table (SDD 3.1)**
   - What we know: CONTEXT.md specifics note these are v2/social features. CONTEXT.md says "include them in the migration for schema completeness but do not implement the associated logic in Phase 1."
   - What's unclear: Nothing — the guidance is explicit. Include columns in V1, do not write service logic for them.
   - Recommendation: Include `level INT DEFAULT 1` and `experience INT DEFAULT 0` in V1 migration. No service code. Document as v2 extension point.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java (JVM) | Backend runtime | Available | 24.0.2 (higher than required 21 LTS) | — |
| Node.js | Frontend build | Available | v22.19.0 | — |
| npm | Frontend packages | Available | 10.9.3 | — |
| Docker | MySQL + Redis containers | Available (Colima) | 29.0.4 | — |
| Gradle | Backend build | Available | 8.11 | Use Gradle Wrapper (gradlew) — recommended |
| MySQL 8.0 | Database | Not directly installed; via Docker | — | Docker Compose (confirmed Docker available) |
| Redis 7 | Token storage, rate limit | Not directly installed; via Docker | — | Docker Compose |
| SMTP server | Email delivery | Not available locally | — | Mailhog via Docker for dev (add to docker-compose.yml) |

**Note on Java version:** Java 24.0.2 is installed, which exceeds the required Java 21 LTS. Spring Boot 3.5.13 supports Java 17-24. However, specify `jvmToolchain(21)` in `build.gradle.kts` to pin the bytecode target to 21 LTS for compatibility with future deployment targets.

**Missing dependencies with no fallback:**
- None — Docker is available for MySQL and Redis.

**Missing dependencies with fallback:**
- SMTP: Use Mailhog in Docker Compose for local dev. Production SMTP via environment variable at deployment time.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework (Backend) | JUnit 5 + MockK 1.13.x + Testcontainers 1.20.x |
| Framework (Frontend) | Vitest + React Testing Library |
| Config file (Backend) | `src/test/resources/application-test.yml` (Testcontainers overrides via `@DynamicPropertySource`) |
| Config file (Frontend) | `vite.config.ts` (test section) — zero-config with react-swc-ts |
| Quick run command | `./gradlew test --tests "*.unit.*"` (backend unit only, no containers) |
| Full suite command | `./gradlew test` (backend) + `npm run test` (frontend) |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | Register with email + password; bcrypt salt 12 applied | Unit + Integration | `./gradlew test --tests "*.AuthServiceTest"` | Wave 0 |
| AUTH-01 | Duplicate email rejected with 409 | Integration | `./gradlew test --tests "*.AuthControllerIntegrationTest"` | Wave 0 |
| AUTH-02 | Verification email sent after registration | Unit (MockK mail) | `./gradlew test --tests "*.EmailVerificationServiceTest"` | Wave 0 |
| AUTH-02 | Clicking verification link marks user as verified | Integration | `./gradlew test --tests "*.AuthControllerIntegrationTest"` | Wave 0 |
| AUTH-02 | Expired token (>24h) rejected | Unit | `./gradlew test --tests "*.EmailVerificationServiceTest"` | Wave 0 |
| AUTH-03 | Password reset request sends email | Unit (MockK mail) | `./gradlew test --tests "*.PasswordResetServiceTest"` | Wave 0 |
| AUTH-03 | Reset token expires after 30 minutes | Unit | `./gradlew test --tests "*.PasswordResetServiceTest"` | Wave 0 |
| AUTH-03 | Rate limit: 4th request in 1h returns 429 | Integration (Redis) | `./gradlew test --tests "*.PasswordResetIntegrationTest"` | Wave 0 |
| AUTH-03 | Unknown email returns 200 (no enumeration) | Integration | `./gradlew test --tests "*.PasswordResetIntegrationTest"` | Wave 0 |
| AUTH-04 | Login returns access token in body + refresh cookie | Integration | `./gradlew test --tests "*.AuthControllerIntegrationTest"` | Wave 0 |
| AUTH-04 | Refresh rotates token; old token rejected | Integration | `./gradlew test --tests "*.TokenRotationIntegrationTest"` | Wave 0 |
| AUTH-04 | Replayed old refresh token invalidates all tokens (reuse detection) | Integration | `./gradlew test --tests "*.TokenRotationIntegrationTest"` | Wave 0 |
| AUTH-04 | Access token works for 15min; 401 after expiry | Unit (mocked clock) | `./gradlew test --tests "*.TokenServiceTest"` | Wave 0 |
| AUTH-04 | Frontend: silent refresh on 401 re-issues request | Frontend Integration | `npm run test -- TokenRefresh` | Wave 0 |
| ACCT-01 | Registration creates account with balance_krw = 100,000,000 | Integration | `./gradlew test --tests "*.AuthControllerIntegrationTest"` | Wave 0 |
| ACCT-01 | Account creation failure rolls back user creation | Integration | `./gradlew test --tests "*.RegistrationAtomicityTest"` | Wave 0 |
| ACCT-01 | Dashboard shows 100,000,000 KRW after login | Frontend E2E (manual) | Manual browser test | N/A |

### Sampling Rate

- **Per task commit:** `./gradlew test --tests "*.unit.*"` (< 30s, no containers)
- **Per wave merge:** `./gradlew test` (backend full suite, ~2-3 min with Testcontainers) + `npm run test` (frontend, < 30s)
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps (Files to Create)

- [ ] `src/test/kotlin/com/stockbot/auth/AuthServiceTest.kt` — unit tests for register, login (MockK)
- [ ] `src/test/kotlin/com/stockbot/auth/TokenServiceTest.kt` — JWT issue, expiry, rotation (MockK clock)
- [ ] `src/test/kotlin/com/stockbot/auth/EmailVerificationServiceTest.kt` — send, verify, expiry (MockK mail)
- [ ] `src/test/kotlin/com/stockbot/auth/PasswordResetServiceTest.kt` — send, verify, rate limit (MockK Redis)
- [ ] `src/test/kotlin/com/stockbot/auth/AuthControllerIntegrationTest.kt` — register, login, refresh, verify email (Testcontainers)
- [ ] `src/test/kotlin/com/stockbot/auth/TokenRotationIntegrationTest.kt` — rotation + reuse detection (Testcontainers + Redis)
- [ ] `src/test/kotlin/com/stockbot/auth/PasswordResetIntegrationTest.kt` — rate limit, no-enumeration (Testcontainers)
- [ ] `src/test/kotlin/com/stockbot/auth/RegistrationAtomicityTest.kt` — account rollback on failure (Testcontainers)
- [ ] `src/test/resources/application-test.yml` — Testcontainers datasource config
- [ ] `src/test/kotlin/com/stockbot/TestcontainersConfig.kt` — shared `@Container` config
- [ ] `src/test/kotlin/com/stockbot/account/AccountControllerIntegrationTest.kt` — GET /accounts (Testcontainers)
- [ ] `src/test/typescript/TokenRefresh.test.tsx` — axios interceptor silent refresh behavior (Vitest)

---

## Sources

### Primary (HIGH confidence)
- `docs/planning/sdd.md` — Project SDD: `users` table (3.1), `accounts` table (3.2), auth API (5.1), error format (6.3)
- `.planning/phases/01-foundation-and-authentication/01-CONTEXT.md` — User decisions D-01 through D-10
- `CLAUDE.md` — Technology stack with exact versions and "What NOT to Use" list
- `.planning/research/STACK.md` — Version research (Spring Boot 3.5.13, jjwt 0.13.0, React 19.2.x)
- `.planning/research/PITFALLS.md` — Float/BigDecimal pitfall (Pitfall 3), Redis key conventions (Pitfall 9)
- `.planning/research/ARCHITECTURE.md` — Atomic order-fill pattern adapted to atomic user+account creation, SecurityFilterChain config pattern

### Secondary (MEDIUM confidence)
- jjwt 0.13.0 — Modular split: `jjwt-api` compile, `jjwt-impl` + `jjwt-jackson` runtime (confirmed in CLAUDE.md)
- Spring Security 6 — `SecurityFilterChain` replacing `WebSecurityConfigurerAdapter` (confirmed removed in 6.0)
- Flyway 10.x — `flyway-mysql` artifact required alongside `flyway-core` for MySQL 8.0 (confirmed in CLAUDE.md version compatibility table)
- Testcontainers MySQL 1.20.x — confirmed in STACK.md

### Tertiary (LOW confidence — flag for validation)
- Mailhog as local SMTP dev tool: widely used community pattern, not verified against official Mailhog project for Spring Boot compatibility. Validate by running `docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog` and confirming Spring Boot mail connection.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — locked by project constraints; versions verified in STACK.md against official sources
- Architecture: HIGH — patterns derive directly from SDD schema and CONTEXT.md decisions
- Pitfalls: HIGH — items 1–6 are specific to this domain and were derived from project research documents + Spring Security 6 documentation
- Test plan: HIGH — maps directly to each requirement ID; test types are conventional for Spring Boot

**Research date:** 2026-03-27
**Valid until:** 2026-04-27 (stable stack — Spring Boot 3.5.x patch releases only; jjwt 0.13.0 stable)

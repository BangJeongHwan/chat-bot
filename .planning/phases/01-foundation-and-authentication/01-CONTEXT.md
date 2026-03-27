# Phase 1: Foundation and Authentication - Context

**Gathered:** 2026-03-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Authenticated users exist and arrive with a funded virtual account ready to trade. This phase delivers: DB schema baseline (users + accounts + supporting auth tables), JWT-based authentication (register, login, token refresh, password reset), email verification, and automatic creation of a KRW 100,000,000 virtual account upon registration. No market data, no trading, no portfolio — only auth and account foundation.

</domain>

<decisions>
## Implementation Decisions

### Registration Flow
- **D-01:** Allow login immediately after registration without email verification. Mark account as `unverified`. Restrict trading operations (Phase 2+) to verified accounts only. This provides better onboarding UX while maintaining financial operation security.
- **D-02:** Email verification uses a clickable link containing a UUID token. Token expires after 24 hours. Clicking the link marks the account as `verified`.

### JWT Token Strategy
- **D-03:** Refresh tokens stored in httpOnly secure cookie (not localStorage). Access tokens returned in response body and stored in memory (React state / Zustand). This prevents XSS access to refresh tokens — critical for a financial service.
- **D-04:** Refresh token rotation on every refresh call. Old refresh token is invalidated immediately when a new one is issued. This prevents token reuse attacks.
- **D-05:** Access Token TTL: 15 minutes. Refresh Token TTL: 7 days. (Locked by project constraints — bcrypt salt 12.)

### Password Reset Flow
- **D-06:** Password reset via email link with UUID token. Token expires after 30 minutes.
- **D-07:** Rate limit: max 3 password reset requests per hour per email address. Prevents abuse without blocking legitimate users.

### DB Schema Scope
- **D-08:** Flyway migrations in Phase 1 include only Phase 1 tables: `users`, `accounts`, `refresh_tokens`, `email_verifications`, `password_reset_tokens`. Other tables (orders, holdings, stocks, etc.) are added in their respective phases. Incremental schema evolution — each phase owns its migrations.
- **D-09:** Use `CHAR(36)` for UUID primary keys as defined in SDD. Use `DECIMAL(18,2)` for KRW balances, `DECIMAL(18,4)` for USD balances, consistent with SDD schema.

### Account Auto-Creation
- **D-10:** On successful registration, automatically create one default account with `balance_krw = 100,000,000`, `balance_usd = 0`, `is_default = true`, `name = '기본 계좌'`. This is atomic with user creation (same transaction).

### Claude's Discretion
- Error response format (standard Spring Boot error structure vs custom)
- Email template design and content
- Flyway migration versioning scheme
- Package structure within Kotlin backend
- Test strategy balance between unit and integration tests

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### System Design
- `docs/planning/sdd.md` — Full system design document with ERD (sections 3.1-3.12), API design (section 5), and architecture (section 7). Phase 1 should follow the users table (3.1), accounts table (3.2), and auth API (5.1) specifications.

### Project Planning
- `.planning/PROJECT.md` — Core value, constraints, and key decisions
- `.planning/REQUIREMENTS.md` — AUTH-01 through AUTH-04, ACCT-01 are Phase 1 requirements
- `.planning/ROADMAP.md` — Phase 1 success criteria (5 items)
- `CLAUDE.md` — Technology stack with exact versions, library choices, and "What NOT to Use" guardrails

### Research
- `.planning/research/STACK.md` — Technology stack research and version recommendations
- `.planning/research/ARCHITECTURE.md` — Architecture patterns and decisions
- `.planning/research/PITFALLS.md` — Known pitfalls to avoid

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None — greenfield project. No existing code.

### Established Patterns
- None yet. Phase 1 will ESTABLISH the patterns for subsequent phases:
  - Backend package structure (Kotlin + Spring Boot)
  - Entity/repository/service/controller layering
  - Exception handling and error response format
  - Test patterns (MockK for unit, Testcontainers for integration)
  - Frontend project structure (React + TypeScript + Vite)

### Integration Points
- Docker Compose for local dev (MySQL 8.0 + Redis 7)
- Gradle Kotlin DSL build configuration
- Vite + React scaffold

</code_context>

<specifics>
## Specific Ideas

- SDD defines detailed table schemas — follow them closely but adapt column types per CLAUDE.md guidance (e.g., avoid `Float`/`Double`, use `BigDecimal`)
- SDD mentions Socket.IO for real-time — this is overridden by CLAUDE.md: use Spring STOMP WebSocket (Phase 4+, not Phase 1)
- SDD uses `CHAR(36)` for UUIDs — acceptable for Phase 1, consider `BINARY(16)` optimization in later phases if needed
- The `users` table in SDD includes `level`, `experience`, `provider`, `provider_id` columns — these are v2/social features. Include them in the migration for schema completeness but do not implement the associated logic in Phase 1.

</specifics>

<deferred>
## Deferred Ideas

- OAuth social login (Google, Kakao, Apple) — AUTH-V2-01, separate phase
- Profile management (nickname, image, investment style) — AUTH-V2-02, separate phase
- Multiple account creation — ACCT-V2-01, separate phase
- Account reset — ACCT-V2-02, separate phase

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-foundation-and-authentication*
*Context gathered: 2026-03-27*

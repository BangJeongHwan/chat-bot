---
phase: 1
slug: foundation-and-authentication
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-27
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + MockK (unit), Testcontainers-MySQL + Testcontainers-Redis (integration), Vitest + React Testing Library (frontend) |
| **Config file** | `build.gradle.kts` (backend), `vitest.config.ts` (frontend) — Wave 0 installs |
| **Quick run command** | `./gradlew test --tests "*Unit*"` |
| **Full suite command** | `./gradlew test` |
| **Estimated runtime** | ~30 seconds (unit), ~90 seconds (integration with Testcontainers) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test --tests "*Unit*"`
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | AUTH-01 | integration | `./gradlew test --tests "*Registration*"` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | AUTH-02 | integration | `./gradlew test --tests "*EmailVerification*"` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | AUTH-03 | integration | `./gradlew test --tests "*PasswordReset*"` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | AUTH-04 | integration | `./gradlew test --tests "*TokenRefresh*"` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | ACCT-01 | integration | `./gradlew test --tests "*AccountCreation*"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `build.gradle.kts` — JUnit 5 + MockK + Testcontainers dependencies
- [ ] `src/test/resources/application-test.yml` — test profile configuration
- [ ] `docker-compose.yml` — MySQL 8.0 + Redis 7 + Mailhog for local dev/test

*Planner will populate exact task IDs after plans are created.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Email delivery and link click | AUTH-02 | Requires email client interaction | 1. Register user 2. Check Mailhog UI 3. Click verification link 4. Verify account status changes |
| Password reset email flow | AUTH-03 | Requires email client interaction | 1. Request reset 2. Check Mailhog UI 3. Click reset link 4. Submit new password 5. Login with new password |
| Session persistence across browser refresh | AUTH-04 | Requires browser behavior | 1. Login 2. Refresh browser 3. Verify still authenticated (cookie-based refresh token) |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending

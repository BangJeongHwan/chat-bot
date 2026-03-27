# Phase 1: Foundation and Authentication - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-03-27
**Phase:** 01-foundation-and-authentication
**Areas discussed:** Registration flow, JWT token storage, Password reset flow, DB schema scope
**Mode:** Auto (--auto flag — recommended defaults selected)

---

## Registration Flow

| Option | Description | Selected |
|--------|-------------|----------|
| Require verification before login | Block login until email verified | |
| Allow login, restrict trading | Login immediately, mark unverified, restrict financial ops until verified | [auto] |
| No verification required | Skip email verification entirely | |

**User's choice:** [auto] Allow login immediately, mark as unverified, restrict trading until verified
**Notes:** Recommended default — better onboarding UX while maintaining security for financial operations

| Option | Description | Selected |
|--------|-------------|----------|
| Clickable UUID link | Email contains link with UUID token | [auto] |
| 6-digit code entry | User enters code from email | |

**User's choice:** [auto] Clickable link with UUID token
**Notes:** Standard approach, simpler UX than code entry

---

## JWT Token Storage

| Option | Description | Selected |
|--------|-------------|----------|
| httpOnly secure cookie (refresh) + memory (access) | Refresh in cookie, access in JS memory | [auto] |
| localStorage for both | Both tokens in localStorage | |
| sessionStorage for both | Both tokens in sessionStorage | |

**User's choice:** [auto] httpOnly secure cookie for refresh, memory for access
**Notes:** Prevents XSS access to refresh tokens — critical for financial service

| Option | Description | Selected |
|--------|-------------|----------|
| Rotate on every refresh | New refresh token each time, old invalidated | [auto] |
| Fixed refresh token | Same refresh token until expiry | |

**User's choice:** [auto] Rotate on every refresh call
**Notes:** Prevents token reuse attacks

---

## Password Reset Flow

| Option | Description | Selected |
|--------|-------------|----------|
| 30 minute expiration | Token valid for 30 minutes | [auto] |
| 1 hour expiration | Token valid for 1 hour | |
| 15 minute expiration | Token valid for 15 minutes | |

**User's choice:** [auto] 30 minutes
**Notes:** Standard for financial services

| Option | Description | Selected |
|--------|-------------|----------|
| 3 requests/hour/email | Rate limit password reset requests | [auto] |
| 5 requests/hour/email | More lenient rate limiting | |
| No rate limiting | Unlimited requests | |

**User's choice:** [auto] 3 requests per hour per email
**Notes:** Prevents abuse without blocking legitimate users

---

## DB Schema Scope

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 1 tables only | users, accounts, auth-related tables | [auto] |
| All tables upfront | Create all 12 tables in Phase 1 migrations | |

**User's choice:** [auto] Phase 1 tables only
**Notes:** Incremental schema evolution — each phase owns its migrations

---

## Claude's Discretion

- Error response format
- Email template design
- Flyway migration versioning scheme
- Package structure
- Test strategy balance

## Deferred Ideas

- OAuth social login (Google, Kakao, Apple)
- Profile management
- Multiple account creation/management
- Account reset functionality

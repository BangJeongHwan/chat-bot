# Project Research Summary

**Project:** StockBot — Paper Trading / Virtual Investment Platform (모의투자 플랫폼)
**Domain:** Financial simulation / paper trading SPA with real market data
**Researched:** 2026-03-26
**Confidence:** HIGH

## Executive Summary

StockBot is a paper trading simulator for Korean and US equity markets, where users practice buying and selling stocks using virtual money backed by real market prices. Expert implementations of this class of product follow a layered, dependency-ordered build sequence: authentication and account management must exist before the order engine, the order engine requires a working price data layer, and all portfolio analytics derive from transaction history produced by the order engine. The architecture is emphatically NOT a full exchange matching engine — simulated fills against a Redis-cached last market price deliver 90% of the realism at 10% of the complexity.

The recommended approach is a Kotlin + Spring Boot 3.5 backend with MySQL 8.0 for all mutable financial state (ACID guarantees are non-negotiable), Redis 7 as a price cache and pending-order index, and a React 19 + Vite 8 frontend using TanStack Query for server state. WebSocket real-time price push is deliberately deferred to Phase 4 — REST polling at 3–10 second intervals is fully sufficient for a simulation platform and avoids premature operational complexity. The MVP scope (Phases 1–3) must cover both KOSPI/KOSDAQ and NYSE/NASDAQ markets, market and limit order execution, and a portfolio dashboard with live unrealized P&L before any social or gamification features are added.

The three highest-risk areas are: (1) financial arithmetic — using `Double` or `Float` anywhere in the service layer causes silent, compounding P&L errors that are extremely costly to remediate after launch; (2) the atomic order fill transaction — balance deduction, holdings update, order status, and transaction record must all commit or all roll back in a single `@Transactional` block with `SELECT FOR UPDATE` on the accounts row; and (3) external API rate limits — Alpha Vantage's free tier allows only 25 requests per day, which a handful of concurrent users will exhaust in minutes unless a background caching architecture is designed in from Phase 2. These three pitfalls have HIGH recovery costs and must be addressed before any production traffic.

---

## Key Findings

### Recommended Stack

The stack is largely locked by project constraints (Kotlin, Spring Boot, MySQL, React, TailwindCSS). Within those constraints, research confirms the best-in-class library choices for each role. Spring Boot 3.5.13 is the correct version to use — Spring Boot 3.4 reached EOL in December 2025 and Spring Boot 4.0 requires Jackson 3.x and JUnit 6 ecosystem migrations not worth taking on a greenfield project. Java 21 LTS is the recommended JVM target for virtual thread (Project Loom) support and a smoother future migration to Boot 4.0. Vite 8 (released March 2026 with Rolldown) replaces CRA, which is fully deprecated.

**Core technologies:**
- **Kotlin 2.2.x + Spring Boot 3.5.13:** Backend language and framework — null safety and coroutines eliminate NPE in financial calculations and allow non-blocking external API calls without a reactive stack
- **Java 21 LTS (JVM):** Virtual threads pair with coroutines; minimum required by Boot 3.5 is Java 17 but 21 is strongly preferred
- **MySQL 8.0 with DECIMAL(18,4):** Source of truth for all financial state; ACID transactions enforce order-fill atomicity; `DECIMAL` type maps cleanly to Kotlin `BigDecimal`
- **Redis 7:** Price cache (sub-second TTL), pending limit-order sorted sets by symbol, session storage, leaderboard sorted sets; Lettuce client (non-blocking) is the default and must not be overridden to Jedis
- **React 19.2 + TypeScript 5.7 + Vite 8:** Frontend SPA; TypeScript enforces types on price/quantity fields; Vite 8 with Rolldown gives best build performance in 2026
- **TailwindCSS 4.x:** CSS-first config, no tailwind.config.js; first-party Vite plugin handles integration
- **TanStack Query v5 + Zustand 5:** TanStack Query for server state (caching, polling, optimistic mutations); Zustand for local UI state — Redux Toolkit is overkill for this scope
- **Lightweight Charts 4.x (TradingView):** Canvas-based OHLC/candlestick rendering handles 5,000+ data points; Recharts (SVG) freezes above ~1,000 data points and must not be used for price charts
- **jjwt 0.13.0 (modular split):** Access token (15 min) + refresh token (7 days); use `jjwt-api` + `jjwt-impl` + `jjwt-jackson` — the legacy single-artifact jjwt is deprecated
- **Flyway 10.x:** SQL-first schema migrations; H2 must NOT be used for integration tests — use `testcontainers-mysql` with real MySQL 8.0 to avoid DECIMAL and datetime divergence bugs

Key library cautions: do not use Socket.IO (no Spring server-side support), do not use `WebSecurityConfigurerAdapter` (removed in Spring Security 6.0), do not use `javax.persistence.*` imports (replaced by `jakarta.persistence.*` in Boot 3.x), and do not use Alpha Vantage free tier as a direct synchronous data source.

### Expected Features

Research cross-referenced against competitor analysis (KRX 모의투자, Kiwoom 모의투자, thinkorswim paperMoney) and the project SDD to confirm MVP scope. The feature dependency chain is: Auth → Account → Order Engine → Price Feed → Portfolio Dashboard. Every P1 feature feeds this chain.

**Must have (table stakes — Phase 1–3):**
- Email/password + OAuth registration and login
- Default virtual account (1억 KRW) auto-created on signup
- KOSPI/KOSDAQ and NYSE/NASDAQ stock master seeded
- Real-time current price via REST polling (3–10s interval)
- Market order and limit order execution with atomic fill
- Trading hours enforcement (KST 09:00–15:30 domestic; ET 09:30–16:00 US with DST handling)
- Fee simulation (0.015%–0.5% deducted at fill time)
- Portfolio dashboard: total assets, unrealized P&L, return percentage
- Holdings list with average buy price and per-holding unrealized P&L
- Transaction history (paginated)
- Stock search by Korean name, English name, and ticker symbol
- Candlestick chart with 1m/5m/15m/1h/daily/weekly timeframes
- Order book (호가창) with 10-level bid/ask depth
- Stock detail page (PER, PBR, EPS, market cap, sector)
- Watchlist (single group)
- Account reset
- FX rate application for USD-denominated holdings (BOK API, cached 60 min)
- Pending order management (view, cancel, modify)

**Should have (competitive — Phase 4, add after validation):**
- Multiple virtual accounts (strategy segmentation)
- Stop and Stop-Limit orders; GTC order support
- WebSocket real-time price push (replace polling)
- Benchmark comparison chart (vs KOSPI / S&P 500)
- Risk metrics: Sharpe Ratio, MDD, Volatility (requires daily portfolio snapshots)
- Sector/industry allocation breakdown
- Daily portfolio snapshots (prerequisite for risk metrics and leaderboard)
- Leaderboard — daily/weekly/monthly return ranking via Redis Sorted Sets
- User level and badge system (XP gamification)
- Price alert and in-app notification system
- Multiple watchlist groups

**Defer (v2+):**
- Community / stock discussion forum (moderation cost disproportionate to v1 size)
- React Native mobile app (defer until web flows are stable)
- News feed integration (analytical value unvalidated)
- Market replay / historical simulation (storage cost too high)
- Algorithmic / automated trading (separate product surface)
- Options / derivatives simulation (separate domain)
- Margin / leverage simulation (requires separate risk model with safety design)

### Architecture Approach

The architecture follows a strict layered build order: Foundation (DB schema, Redis, JWT skeleton) → Auth → Market Data → Order Engine → Pending Order Evaluation → Portfolio → Secondary features → WebSocket push → Social/gamification. The critical architectural insight is that MySQL is the exclusive source of truth for all mutable financial state — Redis is a cache and index, never a data store. Unrealized P&L is never persisted; it is computed on-demand from live prices in the Redis cache. The order fill is a single atomic `@Transactional` block with `REPEATABLE_READ` isolation and `SELECT ... FOR UPDATE` on the accounts row — this is the most safety-critical code path in the system.

**Major components:**
1. **AuthService** — JWT issuance/refresh, OAuth 2.0 (Google/Kakao/Apple), email verification, bcrypt; communicates with MySQL (users) and Redis (refresh token blacklist)
2. **MarketDataService** — Polls KIS (domestic) and Alpha Vantage/Yahoo Finance (international) on background scheduler; writes to Redis price cache with symbol-partitioned TTLs; serves all price, chart, and order book API endpoints; decoupled from order execution
3. **TradeService / FillService** — Validates and executes orders; fills at Redis-cached price (never directly from external API); uses Redis Sorted Set `pending_buy:{symbol}` / `pending_sell:{symbol}` for limit order price monitoring; all fills atomic in one transaction
4. **PortfolioService** — Computes unrealized P&L on-demand with Redis `MGET` for bulk price fetch; persists daily snapshots at market close using official closing prices (종가), not cache; computes realized P&L from `transactions.realized_pnl` using Average Cost method
5. **Scheduler** — EOD pending order auto-cancel (non-GTC); nightly portfolio snapshot trigger; leaderboard rank recalculation
6. **API Gateway (Nginx)** — SSL termination, rate limiting (auth: 10 req/min; orders: 60 req/min), WebSocket proxy upgrade with `proxy_read_timeout 0`

### Critical Pitfalls

1. **Floating-point arithmetic for financial values** — Using `Double` or `Float` anywhere in the service layer causes silent P&L rounding errors that compound over hundreds of trades. Use `java.math.BigDecimal` exclusively, always with `BigDecimal("0.015")` string constructor (not `BigDecimal(0.015)` double constructor), and explicit `RoundingMode.HALF_UP` on division. Define `DECIMAL(18,4)` on all price/amount columns. Recovery cost after launch is HIGH. Must be enforced in Phase 1.

2. **Concurrent order race condition on account balance** — Two simultaneous orders read the same balance before either writes, both pass validation, and the account goes negative. Fix: `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the account repository query — serializes concurrent orders on the same account. For balance reservations, deduct at order placement (not at fill) and refund on cancel. Must be designed in Phase 2.

3. **Alpha Vantage free tier exhaustion** — 25 requests/day means a handful of concurrent users will exhaust the daily quota within minutes. The rate-limit response is HTTP 200 (not 429) with a JSON informational message — a naive HTTP client will treat it as success and serve garbage data. Fix: background polling job writes all active symbols to Redis cache; request handlers read only from cache; Yahoo Finance as primary source for basic quotes; circuit breaker returns stale data with a "delayed" flag on quota exhaustion. Must be designed in Phase 2.

4. **Non-atomic order fill (split transactions)** — Separate service calls for balance deduction, holdings update, and transaction record create windows for partial failure and account corruption. Fix: single `@Transactional` block for all 6 steps of fill execution; any exception rolls back all changes. Architecture Pattern 3 in ARCHITECTURE.md documents the exact execution sequence.

5. **Trading hours validation using server local timezone** — `LocalDateTime.now()` uses JVM default timezone; on a UTC server all market hours checks are wrong. Fix: always use `ZonedDateTime` with `ZoneId.of("Asia/Seoul")` and `ZoneId.of("America/New_York")`; store all timestamps in UTC in MySQL; convert to user-local timezone only at the API response layer. US market DST handling (March/November transitions) must have dedicated unit tests.

---

## Implications for Roadmap

Based on the combined research, the build order mandated by component dependencies suggests a 5-phase structure for this project:

### Phase 1: Foundation and Authentication
**Rationale:** Everything else in the system requires a database schema, Redis connection, error handling baseline, and authenticated users. There is no shortcut — these are structural prerequisites.
**Delivers:** Working user registration/login (email + OAuth), JWT access + refresh token flow, account auto-creation on signup, Flyway-managed schema baseline.
**Addresses:** Email/password registration, OAuth login, default virtual account creation.
**Avoids:** Float arithmetic pitfall — establish `BigDecimal` conventions and Redis key namespacing standards before any financial logic is written. Redis key naming doc should be agreed in this phase.
**Research flag:** Standard patterns. No additional phase research needed; Spring Security 6 JWT setup is well-documented.

### Phase 2: Market Data and Order Execution Engine
**Rationale:** The order engine is the core product value, but it cannot execute without price data (fills need a price source) and cannot be tested without the atomic transaction pattern preventing balance corruption. These two workstreams must be developed together. This phase carries the most pitfall risk of the entire project.
**Delivers:** KIS and Alpha Vantage/Yahoo Finance adapters with Redis caching; stock search and price endpoints; market order and limit order execution; trading hours enforcement; fee simulation; holdings and transaction persistence.
**Uses:** Redis Sorted Sets (`pending_buy:{symbol}`, `pending_sell:{symbol}`) for limit order matching; `@Lock(PESSIMISTIC_WRITE)` on account balance; background scheduler for price polling.
**Implements:** MarketDataService cache-through pattern, TradeService / FillService atomic transaction pattern, Scheduler (EOD order cancel).
**Avoids:** Alpha Vantage rate limit pitfall (background polling, circuit breaker); concurrent balance race condition; trading hours timezone bug; partial-fill average-price drift.
**Research flag:** Needs phase research. KIS Open API integration (authentication flow, WebSocket session pool limits, paper-mode rate limits of 5 req/sec), Alpha Vantage vs Yahoo Finance fallback behavior, and Resilience4j circuit breaker configuration all warrant deeper research before implementation tasks are written.

### Phase 3: Portfolio Dashboard and Analytics
**Rationale:** Once orders can be placed and transactions recorded, the portfolio view is the primary product feedback loop — users need to see the results of their trading before the core experience is complete. Daily snapshot infrastructure is also foundational for all Phase 4 analytics features.
**Delivers:** Portfolio dashboard (total assets, P&L, return %), holdings list with average buy price and unrealized P&L, transaction history timeline, stock detail pages, watchlist, account reset, FX rate application for USD holdings, candlestick chart with multi-timeframe support, order book display.
**Implements:** PortfolioService on-demand P&L calculation (Redis `MGET` bulk fetch), daily portfolio snapshot scheduler using closing prices (종가), FX rate caching (BOK API).
**Avoids:** Snapshot timing inconsistency pitfall — snapshot must use official closing prices, be idempotent, and record `price_source` and `snapshot_time` per row. N+1 price-fetch performance trap on holdings list (use `MGET`, not per-symbol `GET`).
**Research flag:** Standard patterns for portfolio P&L and candlestick chart integration. KIS candlestick data API format may need targeted research.

### Phase 4: Real-Time Push, Advanced Orders, and Social Features
**Rationale:** This phase adds the "power user" layer. WebSocket push replaces REST polling only after core trading is validated and user retention data shows polling latency is a real complaint. Advanced order types (Stop, Stop-Limit, GTC) are added when limit order engagement metrics justify the complexity. Gamification (leaderboard, badges) launches here to address retention drop-off after the first week.
**Delivers:** WebSocket STOMP price push (replaces client-side polling); Stop and Stop-Limit orders; GTC order support; multiple virtual accounts; benchmark comparison chart (vs KOSPI / S&P 500); risk metrics (Sharpe Ratio, MDD, Volatility — requires portfolio snapshots from Phase 3); sector allocation analytics; leaderboard (daily/weekly/monthly, Redis Sorted Set); user level and badge system; price alerts and in-app notifications; multiple watchlist groups.
**Implements:** WebSocket subscription registry (symbol-partitioned `ConcurrentHashMap`, upgrades to Redis Pub/Sub for multi-instance); NotificationService with `@TransactionalEventListener(AFTER_COMMIT)` for fill notifications; leaderboard via Redis Sorted Set scores updated on each fill.
**Avoids:** KIS WebSocket 41-symbol-per-session limit — design session pool from the start of WebSocket implementation; in-memory STOMP broker is sufficient up to ~3,000 concurrent users (upgrade to Redis-backed STOMP broker at 10k+).
**Research flag:** Needs phase research. WebSocket session pool management for KIS (41-symbol limit), Resilience4j configuration for production circuit breaking, Redis Pub/Sub STOMP broker setup for horizontal scaling.

### Phase 5: Scale, Mobile, and Community (v2+)
**Rationale:** Deferred until product-market fit is validated. Scaling adjustments (MySQL read replica, Redis cluster, Kafka for async order fills) are triggered by real user load, not pre-emptively built. React Native mobile app shares API types and hooks from the web frontend. Community features require moderation tooling.
**Delivers:** Horizontal scaling hardening; React Native mobile app; community/discussion forum; news feed integration; market replay (conditional on storage/API cost validation); optional margin simulation.
**Research flag:** All items in this phase need phase research when they are scheduled. No pre-research needed now.

### Phase Ordering Rationale

- Auth before Order Engine: JWTs must be issuable before any protected endpoints exist.
- Market Data co-built with Order Engine: fill price must come from cache, not external API — the cache layer and the consumer must be built together.
- Order Engine before Portfolio: portfolio P&L is derived from transaction records; there is no portfolio without fills.
- Daily snapshots in Phase 3 (not Phase 4): risk metrics and leaderboard (Phase 4) depend on a time series of portfolio values; the snapshot infrastructure must be running for at least a few weeks before analytics are meaningful.
- WebSocket deferred to Phase 4: REST polling at 3–10 seconds is sufficient for simulation and avoids the operational complexity of connection management, session pooling (KIS 41-symbol limit), and horizontal scaling before core trading value is proven.
- Gamification (leaderboard, badges) in Phase 4 alongside WebSocket: both require the same real-time infrastructure (Redis pub/sub, daily snapshots already running) and target the same retention problem.

### Research Flags

Phases requiring deeper research during planning:
- **Phase 2 (Market Data + Order Engine):** KIS Open API authentication flow, paper-mode rate limit (5 req/sec vs 15 req/sec live), WebSocket session acknowledgment behavior, Resilience4j circuit breaker config for Alpha Vantage quota exhaustion
- **Phase 4 (Real-Time + Advanced Orders):** KIS WebSocket 41-symbol session pool design, Redis Pub/Sub STOMP broker wiring for multi-instance deployment, Stop/Stop-Limit order trigger mechanics at scale

Phases with well-established patterns (can skip `research-phase`):
- **Phase 1 (Foundation + Auth):** Spring Security 6 JWT + OAuth2 Resource Server is thoroughly documented; Flyway MySQL setup is standard
- **Phase 3 (Portfolio Dashboard):** P&L calculation patterns and TradingView Lightweight Charts integration are stable and well-documented

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Core stack locked by project constraints; all versions verified against official release pages and changelogs as of 2026-03-26; Boot 3.5.13 release confirmed same day |
| Features | HIGH | Cross-referenced against SDD spec, competitor analysis (KRX, Kiwoom, thinkorswim), and multiple industry sources; MVP scope aligns with standard paper trading platform expectations |
| Architecture | HIGH | Grounded in project's own SDD plus verified patterns from Alpaca Paper Trading (production SaaS), Vlad Mihalcea's Spring transaction guidance, and official Redis documentation |
| Pitfalls | HIGH | Each pitfall backed by primary sources: KIS API limits from direct API documentation, Alpha Vantage limits from macroption.com audit, BigDecimal pitfalls from java-performance.info and multiple practitioner articles |

**Overall confidence:** HIGH

### Gaps to Address

- **KIS paper/demo environment behavior:** Rate limits (5 req/sec confirmed), WebSocket 41-symbol limit confirmed, but the exact authentication flow for simulated account mode (모의투자) vs live account mode needs hands-on verification during Phase 2 implementation. The API key provisioning process for paper trading accounts differs from live accounts.
- **Alpha Vantage vs Yahoo Finance reliability tradeoff:** Yahoo Finance has no official API; undocumented endpoint stability is unverified. During Phase 2 research, evaluate Finnhub (60 req/min free tier) as a more stable alternative to Yahoo Finance for US stock basic quotes.
- **Slippage model calibration:** PITFALLS.md recommends a tiered slippage model based on order size as a percentage of average daily volume. The volume data source for this calculation (which API endpoint from KIS or Alpha Vantage provides reliable average daily volume for the slippage denominator) needs confirmation during Phase 2 research.
- **Portfolio snapshot idempotency under horizontal scaling:** When Phase 5 adds multiple backend instances, the snapshot scheduler must not create duplicate rows. `INSERT ... ON DUPLICATE KEY UPDATE` with a `UNIQUE` constraint on `(account_id, snapshot_date)` is the recommended fix — but Flyway migration for this constraint needs explicit inclusion in Phase 3 schema design.

---

## Sources

### Primary (HIGH confidence)
- [Spring Boot 3.5.13 release](https://spring.io/blog/2026/03/26/spring-boot-3-5-13-available-now/) — version and EOL verified
- [Spring Boot supported versions / EOL](https://github.com/spring-projects/spring-boot/wiki/Supported-Versions) — Boot 3.4 EOL December 2025 confirmed
- [KIS Developers portal](https://apiportal.koreainvestment.com/) — REST + WebSocket API reference
- [Redis Sorted Sets documentation](https://redis.io/docs/latest/develop/data-types/sorted-sets/) — pending order index design
- [Spring @Transactional — Vlad Mihalcea](https://vladmihalcea.com/spring-transaction-best-practices/) — atomic fill transaction pattern
- [Alpaca Paper Trading docs](https://docs.alpaca.markets/docs/paper-trading) — simulated fill vs LOB engine decision
- [jjwt GitHub repository](https://github.com/jwtk/jjwt) — 0.13.0 modular split confirmed
- Project SDD (`docs/planning/sdd.md`) — primary source for schema and service boundaries

### Secondary (MEDIUM confidence)
- [KIS API Throttling — hky035.github.io](https://hky035.github.io/web/kis-api-throttling/) — 5 req/sec paper mode, Guava RateLimiter recommendation
- [KIS WebSocket 41-symbol limit — hky035.github.io](https://hky035.github.io/web/refact-kis-websocket/) — session pool requirement
- [BigDecimal vs Double — java-performance.info](https://java-performance.info/bigdecimal-vs-double-in-financial-calculations/) — financial arithmetic pitfall
- [Pessimistic locking — vladmihalcea.com](https://vladmihalcea.com/how-to-fix-optimistic-locking-race-conditions-with-pessimistic-locking/) — concurrent balance race condition fix
- [QuantConnect Paper Trading](https://www.quantconnect.com/docs/v2/cloud-platform/live-trading/brokerages/quantconnect-paper-trading) — industry standard simulation approaches
- [LuxAlgo — Top 5 Metrics for Trading Strategies](https://www.luxalgo.com/blog/top-5-metrics-for-evaluating-trading-strategies/) — Sharpe Ratio, MDD, Volatility feature rationale

### Tertiary (LOW confidence)
- [Yahoo Finance as backup source] — No official API; use only as last-resort fallback; endpoint stability unverified
- [Alpha Vantage 25 req/day limit — macroption.com](https://www.macroption.com/alpha-vantage-api-limits/) — confirmed as of 2025; verify during Phase 2 that this limit still applies to the free tier

---
*Research completed: 2026-03-26*
*Ready for roadmap: yes*

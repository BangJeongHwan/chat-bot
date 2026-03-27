# Pitfalls Research

**Domain:** Paper trading / virtual investment platform (모의투자 플랫폼)
**Researched:** 2026-03-26
**Confidence:** HIGH

---

## Critical Pitfalls

### Pitfall 1: Market Order Fills at Exact Quote Price (No Slippage Simulation)

**What goes wrong:**
Market orders are filled instantly at the exact last-traded price regardless of order size or market conditions. A user submitting a market buy for 10,000 shares of a thinly-traded stock gets filled at the same price as a 10-share order. This creates systematically optimistic results — strategies that look profitable in simulation fail in live trading because slippage was never modeled.

**Why it happens:**
The simplest implementation is `filled_price = current_price`. Developers correctly use the KIS or Alpha Vantage API to get the current price and use it directly. The missing step is adjusting for volume impact and bid-ask spread.

**How to avoid:**
Implement a tiered slippage model at the order execution layer:
- For orders < 1% of average daily volume: add ±0.05% slippage (spread crossing)
- For orders 1–5% of average daily volume: add ±0.1–0.3% slippage
- For orders > 5% of average daily volume: add ±0.5–1.0% slippage and consider partial fills
- Use bid price for sells, ask price for buys (not mid-price). Bid-ask spread data is available from the KIS 호가창 (order book) endpoint.
- The `filled_price` column in the `orders` table already supports `DECIMAL(18,4)` — use it to store the slippage-adjusted price, not the raw quote price.

**Warning signs:**
- All market orders fill at exactly the price returned from the price API with zero deviation
- Users report "my paper gains never happen in real trading"
- No volume-based logic in the order execution service

**Phase to address:** Phase 2 (Order/Execution Engine). Must be designed in from the start — retrofitting slippage into an existing fill engine requires re-running all historical paper trades to recalculate P&L, which breaks user history.

---

### Pitfall 2: Concurrent Order Processing Race Condition on Account Balance

**What goes wrong:**
A user submits two concurrent BUY orders (e.g., clicking twice, or two browser tabs). Both orders read the same `balance_krw` value (e.g., 50,000,000 KRW), both pass the balance validation check, and both execute — spending 100,000,000 KRW from a 50,000,000 KRW balance. The account goes negative.

**Why it happens:**
The balance check and deduction are done as two separate operations without a database-level lock. With Spring's `@Transactional`, the read and write are in the same transaction, but if two transactions start simultaneously and both read before either writes, both will pass the validation.

**How to avoid:**
Use `SELECT ... FOR UPDATE` (pessimistic locking) on the `accounts` row when processing any order that modifies balance. In Spring Boot + JPA:
```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.id = :id")
fun findByIdForUpdate(id: UUID): Account
```
This serializes concurrent orders on the same account. Alternatively, use an optimistic locking `@Version` field on `Account` entity and catch `OptimisticLockException` to retry — but for financial balance mutations, pessimistic locking is safer and more predictable.

Additionally: deduct balance at order placement time (not at fill time) and refund if the order is cancelled. Never hold balance in an "uncommitted" state.

**Warning signs:**
- Integration tests don't test concurrent order submission
- Balance validation logic reads `balance_krw` without a `FOR UPDATE` hint
- No `@Version` field or explicit lock annotation on Account entity

**Phase to address:** Phase 2 (Order/Execution Engine). This must be addressed before any concurrent load testing. A single-user dev environment will never expose this bug.

---

### Pitfall 3: Floating-Point Arithmetic for Financial Calculations

**What goes wrong:**
Using `Double` or `Float` for price calculations causes invisible rounding errors. Example: `0.1 + 0.2 = 0.30000000000000004` in floating-point. Over hundreds of transactions, these errors accumulate — a user's realized P&L may be off by ±10–100 KRW compared to what the correct value should be. Leaderboard rankings can be distorted if `pnl_rate` is stored or calculated as a floating-point type.

**Why it happens:**
Kotlin's default numeric types are `Double`/`Float` for decimals. Developers use them in service-layer calculations and only convert to `BigDecimal` when calling JPA to persist. But intermediate calculations already have rounding errors before persistence.

**How to avoid:**
- Use `java.math.BigDecimal` for ALL financial arithmetic throughout the application (not just at persistence boundaries)
- Never use `BigDecimal(double)` constructor — always use `BigDecimal("0.015")` or `BigDecimal.valueOf(0.015)`
- Always specify `RoundingMode` explicitly on division: `price.divide(quantity, 4, RoundingMode.HALF_UP)`
- Define a domain-level `Money` value class wrapping `BigDecimal` with pre-set scale for KRW (0 decimal places, round to nearest won) vs USD (4 decimal places)
- The SDD already correctly defines `DECIMAL(18,4)` for price columns — match this scale in the Kotlin entity field types using `@Column(precision = 18, scale = 4)`

**Warning signs:**
- Any `Double` or `Float` variable in service-layer code handling price, fee, or P&L
- Fee calculation using `price * quantity * 0.00015` without explicit `BigDecimal`
- Unit tests for P&L that use `assertEquals(expected, actual)` instead of `assertThat(actual).isEqualByComparingTo(expected)`

**Phase to address:** Phase 1 (Foundation/Infrastructure) — define a `Money` type or BigDecimal conventions before any financial logic is written. Impossible to fix safely after many calculations are in production.

---

### Pitfall 4: Alpha Vantage Free Tier Hard Limit Breaking the Application

**What goes wrong:**
Alpha Vantage free tier is limited to **25 API requests per day** (5 per minute). A single user viewing 5 stock charts or a watchlist of 10 foreign stocks will exhaust the daily quota within minutes of the service starting. When the limit is hit, all overseas stock data returns errors, and the entire US stock trading feature effectively goes down until midnight UTC.

**Why it happens:**
Developers test with a single stock symbol during development and never hit the limit. The free tier feels adequate for dev/test. The application goes live and 5 concurrent users immediately exhaust the daily quota.

**How to avoid:**
- Never call Alpha Vantage per user request — always cache responses in Redis with TTL matching the data update frequency (minimum 60-second TTL for quotes, longer for historical candles)
- Implement a background polling service (scheduled job) that fetches prices for all "active" foreign stocks and writes to Redis. Individual requests read from cache only.
- Use Yahoo Finance as the primary source for foreign stocks (no rate limit for basic quotes), Alpha Vantage only for data Yahoo Finance doesn't provide (e.g., detailed fundamentals)
- Budget API calls explicitly: if you have 300 active foreign stocks, 25 daily calls covers only 25 symbols once per day — a premium plan or alternative source is mandatory for production
- Implement circuit breaker logic so Alpha Vantage failures do NOT cascade to application errors — return cached (stale) data with a "data may be delayed" flag

**Warning signs:**
- Alpha Vantage API calls made synchronously inside request handlers
- No Redis caching layer for foreign stock prices
- Application throws 500 errors when Alpha Vantage returns `{ "Information": "Thank you for using Alpha Vantage!" }` (the rate limit response)

**Phase to address:** Phase 2 (Market Data). Must be designed as a background polling architecture from day one. Adding caching after the fact requires restructuring the entire data access layer.

---

### Pitfall 5: Trading Hours Validation Using Server's Local Timezone

**What goes wrong:**
The server validates Korean market hours (09:00–15:30) using the server's local time instead of KST explicitly. If the server is on AWS us-east-1 (UTC-5/UTC-4), the validation window is completely wrong — users can place orders at 00:00 UTC (09:00 KST) but the server thinks it's 19:00 and rejects the order. For US markets (NYSE 09:30–16:00 ET), Daylight Saving Time shifts the Korean-equivalent times by one hour (22:30 KST in winter vs 23:30 KST in summer), and failing to handle DST causes a 1-hour gap where valid US orders are rejected.

**Why it happens:**
`LocalDateTime.now()` in Java/Kotlin uses the JVM default timezone. Developers test locally in KST and it works fine. The bug only appears on UTC servers or when DST transitions occur.

**How to avoid:**
- Always use `ZonedDateTime` with explicit timezone, never `LocalDateTime` for market hours logic
- Define timezone constants: `ZoneId.of("Asia/Seoul")` for KRW markets, `ZoneId.of("America/New_York")` for US markets
- US market hours DST logic: NYSE is always 09:30–16:00 ET — use `America/New_York` zone and Java handles DST automatically
- Store all `ordered_at`, `filled_at`, and `executed_at` timestamps in UTC in the database (MySQL DATETIME → UTC)
- Display times in the user's local timezone (KST for Korean users) only at the UI/API response layer
- Write dedicated unit tests for market hours validation covering DST transition dates (second Sunday of March and first Sunday of November for US)

**Warning signs:**
- `LocalDateTime.now()` anywhere near trading hours validation code
- Server timezone not explicitly set in Docker/Kubernetes deployment config
- No test cases for DST transition dates or non-KST server deployment

**Phase to address:** Phase 2 (Order/Execution Engine). Timezone logic is baked into the trading hours validation — must be correct from the first implementation.

---

### Pitfall 6: Inconsistent Portfolio Snapshot Timing Causing P&L Anomalies

**What goes wrong:**
Daily P&L is calculated by comparing `portfolio_snapshots.total_value` at day end vs previous day end. If snapshots are taken at inconsistent times (some at 15:30 KST close, some at 23:59, some mid-day due to scheduler drift) or use stale Redis-cached prices rather than actual closing prices, the daily P&L graph shows unexplained spikes or drops. Users see a "loss" on a day where their holdings actually gained value, purely because snapshot A used 15:30 price and snapshot B used 09:05 price the next morning.

**Why it happens:**
Snapshot scheduling via `@Scheduled` cron jobs can drift or be delayed by server load. Developers read current price from Redis cache (which may be 5–10 minutes stale) instead of fetching the official closing price from the market data API.

**How to avoid:**
- Trigger Korean market snapshots only after 15:30 KST (market close) using the official closing price from KIS API (종가 데이터), not the cached last-seen price
- Trigger US market snapshots after 16:00 ET close
- Record which price source and timestamp was used in the snapshot (add `snapshot_time DATETIME` and `price_source VARCHAR(20)` columns to `portfolio_snapshots`)
- Make the snapshot job idempotent — if re-run for the same `snapshot_date`, UPDATE rather than INSERT a duplicate
- For holdings where price data is unavailable (API timeout, holiday), use previous day's closing price with a flag, not a zero or null — zero holdings would show a 100% loss

**Warning signs:**
- `portfolio_snapshots` table has multiple rows for the same `account_id` + `snapshot_date`
- Snapshot scheduler fires at 23:59 KST rather than immediately after market close
- P&L calculation reads from Redis cache without checking cache age/staleness

**Phase to address:** Phase 3 (Portfolio/Analytics). Snapshot consistency is foundational for all analytics features. Define the exact snapshot trigger logic before building any P&L graph feature.

---

### Pitfall 7: KIS API WebSocket Subscription Limit Causing Silent Data Gaps

**What goes wrong:**
KIS API WebSocket allows subscribing to a maximum of **41 symbols per session** for real-time price updates. When Phase 4 enables WebSocket-based real-time prices, a platform with 300+ active stocks silently stops receiving updates for any symbols beyond the 41st. Users on watchlists beyond 41 symbols see frozen prices with no error message.

**Why it happens:**
The 41-symbol limit is not immediately obvious during development when only testing with a handful of symbols. The limit is per-session, so a single WebSocket connection will silently drop subscription confirmations after the 41st symbol.

**How to avoid:**
- Design the WebSocket subscription manager to maintain multiple sessions (each capped at 41 symbols), with a session pool managed per market (KRW market separate from USD market)
- For Phase 1–3 (REST polling), this is not an issue — but architect the price data service with this constraint in mind so it's replaceable in Phase 4
- Monitor subscription acknowledgment responses and alert when subscriptions fail silently
- KIS REST API rate limit for paper/demo (모의투자) is **5 requests per second** vs 20 for live accounts — factor this into polling interval design

**Warning signs:**
- Single WebSocket connection used for all stock symbols
- No logging of subscription acknowledgment/rejection responses from KIS WebSocket
- Price update gaps for symbols beyond alphabetical position ~41 in the subscription list

**Phase to address:** Phase 2 for awareness, Phase 4 for WebSocket implementation. Note the 5 req/sec REST limit applies immediately in Phase 2 polling.

---

### Pitfall 8: Average Buy Price Drift on Partial Fills

**What goes wrong:**
When a limit order is partially filled across multiple price points, the `holdings.avg_buy_price` calculation drifts due to incorrect weighted average computation. Example: Hold 100 shares at 50,000 KRW avg. Buy 50 more shares — first 30 fill at 50,100 and last 20 fill at 50,200. If the code updates `avg_buy_price` twice (once per fill event) using the formula `(old_qty * old_avg + new_qty * fill_price) / total_qty`, the second update uses the already-modified `avg_buy_price` from the first update, which is correct. But if both fill events arrive concurrently and both read the same pre-update `avg_buy_price`, the second update overwrites the first, losing the first partial fill from the average calculation.

**Why it happens:**
Partial fills are represented as multiple `PARTIAL` status updates to the same order. If fill events are processed concurrently (e.g., two message queue consumers), both read the same `holdings` row before either writes, and one update is lost.

**How to avoid:**
- Process fill events for the same order serially — use a per-order lock or a message queue with partition key = `account_id + stock_id`
- Recalculate `avg_buy_price` from scratch using `total_invested / quantity` rather than incrementally updating — this is idempotent and safe to retry
- The `holdings` table has `total_invested DECIMAL(18,4)` — always keep this in sync: `total_invested += fill_price * fill_quantity`, then `avg_buy_price = total_invested / quantity`
- Add a database-level constraint or application check: `avg_buy_price` must always equal `total_invested / quantity` (within rounding tolerance)

**Warning signs:**
- `avg_buy_price` updated in-place without recalculating from `total_invested`
- No locking on `holdings` row during fill processing
- Unit tests that only test single-fill scenarios

**Phase to address:** Phase 2 (Order/Execution Engine) — specifically the fill processing logic for limit orders.

---

### Pitfall 9: Redis Cache Key Collision Between Accounts or Users

**What goes wrong:**
Redis cache keys for stock prices are set as `price:{symbol}` — fine. But if session data or portfolio summaries are cached with keys like `portfolio:{account_id}`, and the account IDs are sequential integers (1, 2, 3), an attacker who knows this pattern can probe adjacent keys and potentially read another user's cached portfolio data. More commonly, a developer caches balance data as `balance:{user_id}` but doesn't invalidate it after an order fills, so the user sees stale balance.

**Why it happens:**
Developers focus on cache hits (performance) and forget to design cache invalidation (correctness). Sequential IDs make key enumeration trivial.

**How to avoid:**
- Use UUIDs (already designed in SDD — `CHAR(36)`) not sequential integers for all entity IDs — this makes cache key enumeration infeasible
- Never cache user-specific financial data (balance, holdings) in Redis without immediate invalidation on every write operation — or simply don't cache it and rely on MySQL with a fast indexed query
- For shared market data (`price:{symbol}`, `ohlcv:{symbol}:{interval}`): set appropriate TTL (e.g., 30 seconds for quotes during market hours, 5 minutes for candle data)
- For user-specific data: use write-through cache or cache-aside with event-driven invalidation, not TTL-based expiry
- Namespace all cache keys: `stockbot:price:KRW:{symbol}`, `stockbot:session:{token_hash}`

**Warning signs:**
- Balance shown in UI does not update immediately after order execution
- Cache keys use sequential integers
- No cache invalidation events on order fill or balance update

**Phase to address:** Phase 2 (Market Data caching) and Phase 3 (Portfolio data). Establish key naming conventions in Phase 1 Foundation.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| `Double` for price calculations in service layer | Faster to write, no BigDecimal verbosity | Silent P&L errors that compound; leaderboard ranking distortion | Never — use BigDecimal from day one |
| Alpha Vantage direct API calls per request | Simple implementation | App-wide outage when daily limit (25 calls) is exhausted within minutes | Never for production |
| `LocalDateTime.now()` for market hours check | Works in dev (KST machine) | Order acceptance/rejection errors on UTC servers and during DST transitions | Never |
| Single portfolio snapshot job at midnight | Simple cron expression | P&L calculated from wrong prices; graphs show false gains/losses | Never — must use market close prices |
| No `FOR UPDATE` lock on balance reads | Avoids lock contention | Double-spend bug allowing negative balance | Never for financial mutations |
| Hard-code commission rate as Double literal (0.00015) | Quick implementation | If rate changes, must find all hardcoded occurrences | MVP acceptable if centralized in one constant file |
| Polling Alpha Vantage per symbol every request | No caching complexity | Daily quota exhausted in minutes | Never in production |
| Snapshot `total_value` using stale Redis price | Fast snapshot creation | P&L discrepancies, user-visible data anomalies | Never — use closing price from market close event |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| KIS Open API (REST) | Calling at full speed until error code `EGW00201` | Pre-throttle to 15 req/sec (live) or 4 req/sec (paper/demo environment) using a rate limiter (Guava `RateLimiter`); implement retry on `EGW00201` with 1-second backoff |
| KIS Open API (WebSocket) | Subscribing all stock symbols on one connection | Cap at 41 symbols per session; maintain a session pool; log subscription acknowledgment for each symbol |
| Alpha Vantage | Making API calls from request handlers synchronously | Background polling job only; Redis cache with ≥60s TTL; circuit breaker returning stale data on quota exhaustion; Yahoo Finance as primary for basic quotes |
| Alpha Vantage | Treating HTTP 200 with JSON `{ "Information": "..." }` as success | Inspect response body — rate limit responses are HTTP 200 with an informational message, not a 429 status code |
| 한국은행 API (환율) | Fetching exchange rate on every USD-denominated calculation | Cache exchange rate in Redis with 1-hour TTL; update once per business day |
| MySQL `DATETIME` | Storing KST timestamps without timezone conversion | Always store UTC in DB; convert to KST at API response layer using `ZonedDateTime` |
| JPA / Hibernate | Using `Double` Kotlin property for `DECIMAL` column | Annotate with `@Column(precision=18, scale=4)` and use `BigDecimal` property type |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| N+1 query on holdings list | Portfolio page slow as users add more stocks | Use `JOIN FETCH` or batch load holdings with stocks in one query | ~20+ holdings per account |
| Snapshot recalculation on every portfolio page load | Portfolio dashboard slow during market hours | Pre-compute and persist daily snapshots; only recalculate current unrealized P&L in real-time | ~5 concurrent users |
| Price lookup for each holding row individually | O(n) API calls for portfolio with n stocks | Batch price fetch; use Redis hash to store all prices in one key `HGETALL stockbot:prices:KRW` | ~10 holdings |
| Leaderboard recalculated by full table scan | Leaderboard page very slow | Use Redis Sorted Set for leaderboard; update score incrementally on each fill event | ~1,000 users |
| KIS API polling for every user's watchlist separately | Rate limit exceeded immediately | Maintain a single canonical "active symbols" set; one background poller for all symbols | >5 concurrent users |
| Concurrent snapshot jobs creating duplicate rows | `portfolio_snapshots` has multiple rows for same date | Make snapshot job idempotent with `INSERT ... ON DUPLICATE KEY UPDATE` | Horizontal scaling / multiple pods |

---

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Cache keys using sequential account IDs | Enumeration attack — probe `balance:1`, `balance:2` etc. to read other users' data | Use UUIDs (already in SDD schema) for all entity IDs; never expose internal numeric IDs |
| API endpoint `GET /orders/{orderId}` without ownership check | User A reads User B's order history by guessing order IDs | Always validate `order.account.userId == authenticatedUserId` in service layer, not just at controller |
| Market data API keys in application.properties without encryption | API key leak in logs or version control | Use Spring Cloud Config / AWS Secrets Manager / Kubernetes Secrets; never commit API keys to git |
| Account balance in JWT payload | Stale balance in token used to bypass real-time validation | Never put financial state in JWT; always read balance from DB with lock on order submission |
| Account reset endpoint with no rate limiting | Attacker resets all user accounts | Add rate limiting on account reset endpoint; require re-authentication (password confirmation) for reset |

---

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Market order executes at price user saw 30 seconds ago | User is surprised when fill price differs from the price they clicked "buy" on | Show a confirmation dialog with "estimated fill price" refreshed at confirmation time; mark market orders as approximate |
| Limit order silently sits as PENDING without status indication | User thinks their order didn't go through; submits duplicate | Show PENDING status prominently with price condition; display live distance-to-trigger (current price vs limit price) |
| P&L displayed in absolute KRW only, not percentage | New users with different initial balances can't compare performance | Show both absolute (±KRW/USD) and percentage return; use percentage for leaderboard |
| USD portfolio shown in USD but total asset shown in KRW using today's rate | Total looks wrong when exchange rate moves | Show USD holdings in USD with KRW equivalent; display exchange rate used and its timestamp |
| Order form shows stale price (Alpha Vantage free tier hit) | User buys at wrong price | Show "price data unavailable" and disable order submission when price data is stale > 5 minutes |
| Trading allowed during US Daylight Saving transition window | User places order at 22:30 KST expecting US open but market doesn't open until 23:30 | Clearly display current market status (OPEN/CLOSED/PRE-MARKET) with local time conversion for each market |

---

## "Looks Done But Isn't" Checklist

- [ ] **Market order execution:** Verify `filled_price != current_price` — slippage is applied based on order size relative to typical volume
- [ ] **Balance deduction:** Verify that submitting two identical orders concurrently results in only one being filled (test with `Thread.sleep` injection or concurrency tests)
- [ ] **Financial arithmetic:** Run `grep -rn "Double\|Float\|\.toDouble()\|\.toFloat()" src/main/kotlin/**/*Service*` and confirm zero results for financial logic
- [ ] **Trading hours (KST):** Deploy to a UTC server and verify Korean market orders are accepted 09:00–15:30 KST and rejected outside those hours
- [ ] **Trading hours (DST):** Verify US market orders are accepted at 22:30 KST in summer (EDT) and 23:30 KST in winter (EST) — test around March and November DST transitions
- [ ] **Alpha Vantage quota:** Verify application continues serving stale data (not errors) when daily quota is exhausted — simulate by setting invalid API key
- [ ] **Snapshot P&L:** Verify portfolio snapshot uses official closing price (종가), not last-seen Redis cache price — check `price_source` column in snapshot
- [ ] **Average buy price:** Verify `avg_buy_price * quantity == total_invested` (within DECIMAL rounding) for all holdings rows after a series of partial fills
- [ ] **Redis key namespacing:** Verify no numeric sequential IDs used in Redis cache keys for user-specific data
- [ ] **Timezone consistency:** Verify all `executed_at` timestamps in `transactions` table are stored in UTC — select a row and confirm it matches expected UTC equivalent of the KST fill time

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| BigDecimal/Float discovered after launch | HIGH | Audit all P&L values; recompute correct values from raw `transactions` table; notify affected users; requires BigDecimal rewrite of all financial service code |
| Race condition causing negative balance discovered | HIGH | Identify affected accounts; manually audit transaction history; apply corrective balance adjustments; implement locking on hot-fix branch |
| Alpha Vantage quota exhausted | LOW | Switch to Yahoo Finance fallback immediately; implement Redis caching before re-enabling Alpha Vantage; or upgrade to paid tier ($50/month) |
| Slippage not simulated | MEDIUM | Add slippage model to fill engine; decide whether to retroactively adjust past fills or only apply going forward (retroactive adjustment breaks user history) |
| Snapshot timing inconsistency discovered | MEDIUM | Re-run snapshot job for affected dates using closing prices; add data migration script; idempotent re-run is safe if snapshot logic is correct |
| KIS rate limit errors causing data gaps | LOW | Implement Guava RateLimiter at 15 req/sec; add retry logic; errors should self-resolve within 1 second |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Float arithmetic for financials | Phase 1 (Foundation) | `grep` for `Double`/`Float` in service layer returns zero results; BigDecimal unit tests pass |
| Redis key naming conventions | Phase 1 (Foundation) | Cache key format document agreed; reviewed in code |
| Alpha Vantage rate limit architecture | Phase 2 (Market Data) | Load test with 10 concurrent users making stock lookups; no API quota errors; Redis hit rate > 95% |
| KIS API throttling (5 req/sec paper mode) | Phase 2 (Market Data) | Rate limiter configured; no `EGW00201` errors in logs under normal load |
| Concurrent order race condition | Phase 2 (Order Engine) | Concurrency test: 10 simultaneous orders on one account; exactly 1 succeeds or balance never goes negative |
| Slippage simulation | Phase 2 (Order Engine) | Market order fill price differs from raw quote price; fill price correlates with order size |
| Trading hours + DST | Phase 2 (Order Engine) | Unit tests for market hours with explicit KST/ET timezone assertions; DST transition dates tested |
| Partial fill avg price drift | Phase 2 (Order Engine) | Integration test: 3 partial fills on same order; final `avg_buy_price == total_invested / quantity` |
| Portfolio snapshot consistency | Phase 3 (Portfolio) | Snapshot `total_value` matches sum of `holdings * closing_price + cash_balance` for a known date |
| WebSocket 41-symbol limit | Phase 4 (Real-time) | 100-symbol subscription test across session pool; all symbols receive price updates |

---

## Sources

- [KIS API Throttling — hky035.github.io](https://hky035.github.io/web/kis-api-throttling/) — KIS REST API rate limits: 20 req/sec (live), 5 req/sec (paper/demo). Guava RateLimiter at 15 req/sec recommended. Error code `EGW00201` indicates rate limit exceeded.
- [KIS WebSocket multi-account — hky035.github.io](https://hky035.github.io/web/refact-kis-websocket/) — 41 symbols per WebSocket session limit
- [Alpha Vantage Rate Limits — macroption.com](https://www.macroption.com/alpha-vantage-api-limits/) — 25 requests/day, 5 requests/minute on free tier
- [Alpha Vantage Premium — alphavantage.co](https://www.alphavantage.co/premium/) — Free tier limitations and premium tier options
- [BigDecimal vs Double — java-performance.info](https://java-performance.info/bigdecimal-vs-double-in-financial-calculations/) — Binary representation issues with Double in financial calculations
- [Stop Using Double for Financial Systems — Medium](https://medium.com/@samrat.alam/stop-using-double-for-financial-system-in-java-bigdecimal-the-right-way-209d58ac673a) — BigDecimal best practices for Java/Kotlin
- [Optimistic vs Pessimistic Locking — codewiz.info](https://codewiz.info/blog/locking-strategies-spring-boot/) — Locking strategies for concurrent order processing in Spring Boot
- [Pessimistic Locking Fix — vladmihalcea.com](https://vladmihalcea.com/how-to-fix-optimistic-locking-race-conditions-with-pessimistic-locking/) — When to use pessimistic locking over optimistic for financial mutations
- [Redis Cache Invalidation — redis.io](https://redis.io/glossary/cache-invalidation/) — Cache invalidation strategies for real-time data
- [Redis Cache Pitfalls — railsdrop.com](https://railsdrop.com/2025/09/10/redis-cache-invalidation-testing-pitfalls-and-checklist/) — Common cache invalidation mistakes
- [Paper Trading Realism Gap — luxalgo.com](https://www.luxalgo.com/blog/paper-trading-how-simulators-prepare-you-for-live-markets/) — Why paper trades don't match live trades (slippage, partial fills)
- [QuantConnect Paper Trading — quantconnect.com](https://www.quantconnect.com/docs/v2/cloud-platform/live-trading/brokerages/quantconnect-paper-trading) — Industry standard approaches to paper trading simulation
- [Paper Trading Partial Fills — forum.alpaca.markets](https://forum.alpaca.markets/t/alpaca-paper-trading-partial-order-fill/2683) — Real-world partial fill issues in paper trading platforms

---
*Pitfalls research for: Paper trading / virtual investment platform (모의투자 플랫폼)*
*Researched: 2026-03-26*

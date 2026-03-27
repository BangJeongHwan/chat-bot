# Architecture Research

**Domain:** Paper Trading / Virtual Investment Platform (모의투자 플랫폼)
**Researched:** 2026-03-26
**Confidence:** HIGH — architecture grounded in project's own SDD plus verified patterns

---

## Standard Architecture

### System Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                         Client Layer                              │
│  ┌──────────────────────────┐  ┌──────────────────────────────┐  │
│  │     React Web (Phase 1)  │  │  React Native Mobile (Ph. 5) │  │
│  │  Multi-panel: Chart +    │  │  Tab-nav, push notifications │  │
│  │  Orderbook + Order form  │  │  Biometric auth              │  │
│  └────────────┬─────────────┘  └──────────────┬───────────────┘  │
└───────────────┼──────────────────────────────┼───────────────────┘
                │ HTTPS REST + WebSocket (Phase 4)
┌───────────────┼──────────────────────────────────────────────────┐
│         API Gateway — Nginx                                        │
│         Rate Limiting / SSL Termination / CORS                    │
│         Auth: 10 req/min  |  Order: 60 req/min                    │
└───────────────┬──────────────────────────────────────────────────┘
                │
┌───────────────┼──────────────────────────────────────────────────┐
│         Backend — Kotlin + Spring Boot                             │
│                                                                    │
│  ┌─────────────┐  ┌──────────────────┐  ┌─────────────────────┐  │
│  │ AuthService │  │  TradeService    │  │  MarketDataService  │  │
│  │             │  │  (Order Engine)  │  │                     │  │
│  │ JWT + OAuth │  │  ┌────────────┐  │  │  KIS API (domestic) │  │
│  │ bcrypt      │  │  │ Fill Logic │  │  │  Alpha Vantage (US) │  │
│  └─────────────┘  │  │ Pending Q  │  │  │  BOK API (FX)       │  │
│                   │  └────────────┘  │  │  Redis price cache  │  │
│  ┌─────────────┐  └──────────────────┘  └─────────────────────┘  │
│  │PortfolioSvc │  ┌──────────────────┐  ┌─────────────────────┐  │
│  │             │  │ NotificationSvc  │  │  Scheduler          │  │
│  │ PnL calc    │  │ In-App / Push    │  │  EOD pending cancel │  │
│  │ Snapshots   │  │ Price alerts     │  │  Daily snapshot     │  │
│  └─────────────┘  └──────────────────┘  │  Leaderboard update │  │
│                                          └─────────────────────┘  │
└───────────────┬──────────────────────────────────────────────────┘
                │
┌───────────────┼──────────────────────────────────────────────────┐
│         Data Layer                                                 │
│  ┌────────────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │  MySQL 8.0     │  │  Redis       │  │  External Market APIs │ │
│  │  (Source of    │  │  - Price TTL │  │  KIS / Alpha Vantage  │ │
│  │   truth for    │  │    cache     │  │  Yahoo Finance (bkup) │ │
│  │   all state)   │  │  - Pending   │  │  BOK (FX)             │ │
│  │                │  │    order set │  │                       │ │
│  │  ACID txns     │  │  - WS subs   │  │  Rate limits apply    │ │
│  └────────────────┘  │  - Sessions  │  └───────────────────────┘ │
│                       └──────────────┘                            │
└──────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Communicates With |
|-----------|----------------|-------------------|
| **AuthService** | Registration, login, JWT issue/refresh, OAuth 2.0 (Google/Kakao/Apple), email verification, password reset | MySQL (users), Redis (refresh token blacklist) |
| **TradeService (Order Engine)** | Order validation, simulated fill execution, balance deduction/credit, holdings update, transaction record, pending order lifecycle | MySQL (orders, accounts, holdings, transactions), Redis (pending order price check set), MarketDataService |
| **MarketDataService** | Fetch and cache real-time prices from KIS/Alpha Vantage, serve price/chart/orderbook APIs, broadcast price updates | Redis (price cache), External APIs, WebSocket broker |
| **PortfolioService** | Compute unrealized/realized P&L, portfolio summary, daily snapshot creation, analytics (sector, Sharpe, MDD) | MySQL (holdings, transactions, portfolio_snapshots), Redis (cached current prices) |
| **NotificationService** | Deliver in-app notifications, monitor price alert thresholds, future: FCM/APNs push | MySQL (notifications, price_alerts), Redis Pub/Sub |
| **Scheduler** | End-of-day pending order auto-cancel (non-GTC), nightly portfolio snapshot, leaderboard rank recalculation | TradeService, PortfolioService, MySQL |
| **API Gateway (Nginx)** | SSL termination, rate limiting per endpoint category, WebSocket proxy upgrade | Backend services |

---

## Recommended Project Structure

```
stockbot-backend/
├── src/main/kotlin/com/stockbot/
│   ├── auth/               # AuthService, JWT filter, OAuth handlers
│   │   ├── controller/
│   │   ├── service/
│   │   └── security/
│   ├── trade/              # Order Engine — most critical domain
│   │   ├── controller/
│   │   ├── service/
│   │   │   ├── OrderService.kt       # submit, cancel, modify
│   │   │   ├── FillService.kt        # simulated fill execution
│   │   │   └── BalanceService.kt     # balance deduction/credit
│   │   └── domain/                   # Order, Holding, Transaction
│   ├── market/             # MarketDataService
│   │   ├── adapter/        # KIS, AlphaVantage, YahooFinance clients
│   │   ├── cache/          # Redis price cache layer
│   │   └── websocket/      # WebSocket handler + subscription registry
│   ├── portfolio/          # PortfolioService
│   │   ├── service/
│   │   │   ├── PnLCalculator.kt
│   │   │   └── SnapshotService.kt
│   │   └── controller/
│   ├── notification/       # NotificationService + price alert monitor
│   ├── scheduler/          # Spring @Scheduled tasks
│   ├── account/            # Account CRUD
│   ├── watchlist/          # Watchlist CRUD
│   └── common/             # Error codes, base response wrapper, utils
└── src/main/resources/
    ├── application.yml
    └── db/migration/       # Flyway migrations

stockbot-frontend/
├── src/
│   ├── pages/
│   ├── components/
│   │   ├── chart/
│   │   ├── orderbook/
│   │   └── order-panel/
│   ├── hooks/              # usePrice, useOrders, usePortfolio
│   ├── api/                # REST client (shared with RN later)
│   ├── store/              # Zustand or Redux slices
│   └── types/              # Shared TypeScript types
```

---

## Architectural Patterns

### Pattern 1: Simulated Fill (not a full matching engine)

**What:** Paper trading does not need a real order-book matching engine. Instead, fills are simulated against the last market price from the cache. Market orders fill immediately at current price. Limit/Stop orders are evaluated whenever a new price tick arrives.

**When to use:** Always — a full matching engine (bid/ask book) is unnecessary complexity for a simulation platform. Real brokers use it; simulators validate against market price.

**Trade-offs:**
- Pro: Dramatically simpler. No order book state machine. < 100ms fill target is trivially achievable.
- Con: Does not simulate partial fills or market impact. Acceptable for learning-focused platform.

**Mechanics:**
```
Market order submitted
  → validate balance (SELECT FOR UPDATE on accounts row)
  → fetch current price from Redis cache
  → execute fill at that price
  → single @Transactional block:
       UPDATE accounts (balance -=)
       INSERT/UPDATE holdings (qty +=, avg_price recalc)
       UPDATE orders (status = FILLED)
       INSERT transactions

Limit order submitted
  → validate balance (reserve amount)
  → INSERT order with status = PENDING
  → add to Redis Sorted Set: key=symbol, score=limit_price, value=order_id

On each price tick from MarketDataService
  → ZRANGEBYSCORE to find BUY limits where limit_price >= current_price
  → ZRANGEBYSCORE to find SELL limits where limit_price <= current_price
  → trigger fill for each matched order_id
```

### Pattern 2: Market Data Cache-Through

**What:** MarketDataService fetches prices from KIS (domestic) and Alpha Vantage (international) on a polling interval, writes to Redis with a short TTL, and all internal reads go through Redis first.

**When to use:** KIS and Alpha Vantage both have rate limits. Caching prevents hammering the upstream APIs and gives consistent sub-10ms price reads for fill simulation and portfolio P&L.

**Trade-offs:**
- Pro: Decouples the external API latency from user-facing API latency.
- Con: Price data is up to one polling interval stale (acceptable for simulation).

**Implementation:**
```
Redis key format:  price:{symbol}         TTL: 3s  (domestic, KIS polling every 1-2s)
                   price:{symbol}         TTL: 15s (international, Alpha Vantage polling every 10s)
                   chart:{symbol}:{interval}  TTL: 60s
                   orderbook:{symbol}     TTL: 2s
                   fx:USD_KRW             TTL: 60s (BOK API, updated every minute)
```

### Pattern 3: Atomic Order-Fill Transaction

**What:** The entire fill pipeline (balance deduction, holdings update, order status update, transaction record) must execute within a single MySQL `@Transactional` block with `SERIALIZABLE` or `REPEATABLE_READ` isolation on the accounts row to prevent double-spend.

**When to use:** Every fill execution. This is the single most critical constraint. A partial failure (balance deducted but holdings not updated) corrupts user state permanently.

**Critical path (all within one transaction):**
```
@Transactional(isolation = REPEATABLE_READ)
fun executeFill(orderId, fillPrice, fillQty):
  1. SELECT ... FOR UPDATE ON accounts (lock row)
  2. Validate balance >= fillPrice * fillQty + fee
  3. UPDATE accounts SET balance_krw = balance_krw - (fillPrice * fillQty + fee)
  4. UPDATE orders SET status=FILLED, filled_price=fillPrice, filled_at=now()
  5. INSERT INTO transactions (order_id, price, qty, fee, realized_pnl)
  6. UPSERT holdings:
       - If new: INSERT (qty, avg_buy_price = fillPrice)
       - If existing BUY: UPDATE avg_buy_price = (old_total + new_total) / total_qty
       - If SELL: UPDATE qty -= fillQty, calculate realized_pnl
  7. COMMIT (or rollback all 6 steps on any exception)
```

### Pattern 4: WebSocket Subscription Registry (Phase 4)

**What:** Clients subscribe to specific symbols. The server maintains a registry of `symbol → Set<WebSocketSession>`. When a price tick arrives, the server fans out only to subscribers of that symbol.

**When to use:** Phase 4. In Phases 1–3, REST polling is used instead. The registry pattern avoids broadcasting all prices to all clients.

**Trade-offs:**
- Pro: Efficient fan-out, avoids sending irrelevant ticks.
- Con: In-memory registry dies on server restart. For multi-instance deployments, need Redis Pub/Sub to bridge instances.

**Phase 4 upgrade path:**
```
Single instance:   ConcurrentHashMap<symbol, Set<WebSocketSession>>
Multi-instance:    Redis Pub/Sub channel per symbol
                   Each instance subscribes and re-broadcasts to local sessions
```

### Pattern 5: Portfolio P&L Calculation

**What:** Unrealized P&L is always computed on-demand from current holdings × current price (fetched from Redis cache). Realized P&L is persisted at fill time in `transactions.realized_pnl`. Daily snapshots persist point-in-time portfolio value.

**When to use:** Portfolio dashboard. Never store unrealized P&L in the database — it would go stale immediately.

**Formula:**
```
unrealized_pnl = SUM( (current_price - avg_buy_price) * quantity ) per holding
realized_pnl   = SUM( transactions.realized_pnl ) WHERE account_id = ?
total_pnl      = unrealized_pnl + realized_pnl
total_value    = cash_balance_krw + (cash_balance_usd * fx_rate) + SUM(holding_value)
```

For SELL realized P&L (Average Cost method — simplest and what Korean retail brokers use):
```
realized_pnl = (fill_price - avg_buy_price) * fill_qty - fee
```

---

## Data Flow

### Order Submission Flow (Buy Limit Order)

```
User submits order (React Web)
    ↓ POST /api/v1/accounts/{id}/orders
API Gateway (rate limit check)
    ↓
TradeService.submitOrder()
    ├── Validate JWT, extract user_id
    ├── Load account (check ownership)
    ├── Validate trading hours (domestic 09:00-15:30 / NYSE 09:30-16:00 ET)
    ├── Validate balance (available_balance >= price * qty + estimated_fee)
    ├── @Transactional:
    │     INSERT orders (status=PENDING)
    │     UPDATE accounts SET reserved_balance += price * qty
    ├── Add to Redis Sorted Set: ZADD pending_buy:{symbol} price order_id
    └── Return 201 with order details
```

### Fill Trigger Flow (Limit Order, Price Tick Arrives)

```
MarketDataService polls KIS/AlphaVantage
    ↓ new price tick for symbol
Write to Redis: SET price:{symbol} {price} EX 3
    ↓
Publish to internal channel (Spring ApplicationEvent or Redis Pub/Sub)
    ↓
TradeService.onPriceTick(symbol, currentPrice)
    ├── ZRANGEBYSCORE pending_buy:{symbol} 0 currentPrice  → matching buy limit orders
    ├── ZRANGEBYSCORE pending_sell:{symbol} currentPrice +inf → matching sell limits
    └── For each matched order_id:
          executeFill(orderId, currentPrice, remainingQty)  [atomic transaction above]
          ZREM from Redis sorted set
```

### Portfolio Dashboard Flow

```
User opens portfolio page
    ↓ GET /api/v1/accounts/{id}/portfolio
PortfolioService
    ├── Load holdings from MySQL
    ├── Bulk-fetch current prices from Redis (MGET price:{symbol} ...)
    ├── Fetch FX rate from Redis (GET fx:USD_KRW)
    ├── Compute unrealized_pnl in-memory (no DB write)
    ├── SUM realized_pnl from transactions table
    └── Return assembled portfolio summary (< 200ms p95 target)
```

### WebSocket Price Push Flow (Phase 4)

```
Client connects → WS handshake via Nginx proxy
    ↓ subscribe:price { symbols: ["005930", "AAPL"] }
Server: registry.add(session, ["005930", "AAPL"])

MarketDataService price tick for "005930"
    ↓
Lookup registry: sessions subscribed to "005930"
    ↓
Broadcast price:update { symbol, price, change, changeRate, ts }
    to each subscribed session (< 500ms end-to-end target)
```

---

## Build Order (Component Dependencies)

Building order matters because each layer depends on the one below.

```
Layer 0 (Foundation — must exist before anything)
    MySQL schema + Flyway migrations
    Redis connection + Spring Data Redis config
    Global error handler + JWT filter skeleton

Layer 1 (Auth — gates all other endpoints)
    AuthService: register, login, JWT, refresh token
    User profile CRUD

Layer 2 (Market Data — required by Order Engine for fills)
    MarketDataService: KIS adapter + Alpha Vantage adapter
    Redis price cache write path
    Stock search + price REST endpoints
    External API error handling + fallback (Yahoo Finance)

Layer 3 (Account + Order Engine — core product value)
    AccountService: create, reset, balance management
    TradeService: submit, cancel, modify orders
    FillService: Market order immediate fill
    Fill atomicity: @Transactional with SELECT FOR UPDATE
    Holdings + Transactions persistence

Layer 4 (Pending Order Evaluation)
    Redis Sorted Set for pending limit/stop orders
    Price tick → fill trigger pipeline
    Stop → market order conversion
    EOD auto-cancel scheduler (Scheduler component)

Layer 5 (Portfolio)
    PortfolioService: unrealized P&L on-demand calculation
    Transaction history API
    Daily snapshot scheduler
    Performance chart data (portfolio_snapshots queries)

Layer 6 (Watchlist + Secondary Features)
    Watchlist CRUD
    Stock detail + chart + orderbook endpoints

Layer 7 (Phase 4: Real-Time Push)
    WebSocket handler + subscription registry
    Price tick broadcast
    order:filled notification push
    Price alert monitor (NotificationService)

Layer 8 (Phase 4: Social / Gamification)
    Leaderboard (Redis Sorted Set for rankings)
    Notification delivery
```

**Critical path:** Layer 0 → 1 → 2 → 3. Everything else can be deferred.

---

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0–1k users | Single Spring Boot instance + single MySQL + single Redis. Polling interval 3–5s. |
| 1k–10k users | MySQL Read Replica for portfolio/history queries. Redis cluster. Increase WebSocket thread pool. Consider switching KIS polling to WebSocket stream if available. |
| 10k–100k users | Horizontal scale Spring Boot (stateless). Redis Pub/Sub bridges WebSocket fan-out across instances. Kafka for async order fill events. MySQL sharding by account_id range. |
| 100k+ users | Event-driven architecture: order submission → Kafka topic → fill consumer. Separate read model (CQRS) for portfolio dashboard. |

### Scaling Priorities

1. **First bottleneck:** Portfolio P&L endpoint — N+1 price fetches per holding. Fix: Redis `MGET` bulk fetch (already described in data flow). Single query for all holdings, single `MGET` call.
2. **Second bottleneck:** Pending order evaluation on price tick — linear scan if Redis sorted set grows large. Fix: ZRANGEBYSCORE is O(log N + M), acceptable up to millions of pending orders.
3. **Third bottleneck:** WebSocket fan-out across multiple server instances. Fix: Redis Pub/Sub per symbol channel.

---

## Anti-Patterns

### Anti-Pattern 1: Building a Full Order-Book Matching Engine

**What people do:** Implement a bid/ask LOB (Limit Order Book) matching engine like real exchanges use (price-time priority, partial fills across multiple resting orders).

**Why it's wrong:** Massive complexity increase (separate in-memory data structure, multi-order partial fill logic, thread safety). A paper trading platform needs simulation fidelity, not exchange infrastructure. The added complexity creates bugs and delays shipping.

**Do this instead:** Simulated fill against last market price. Market order → fill at current Redis-cached price. Limit order → fill when price crosses the limit. This is how Alpaca Paper Trading and QuantConnect work.

### Anti-Pattern 2: Storing Unrealized P&L in the Database

**What people do:** Persist `unrealized_pnl` in `holdings` or `accounts` tables and update it on every price tick.

**Why it's wrong:** Creates massive write amplification — every price update for every symbol requires updating every holder's row. At 1k users holding AAPL, that's 1k writes per price tick. Also creates stale state and race conditions.

**Do this instead:** Compute unrealized P&L on-demand in the service layer using `(current_price - avg_buy_price) * quantity`. Current prices come from Redis cache. Never persist this value.

### Anti-Pattern 3: Non-Atomic Order Fill (Split Transactions)

**What people do:** Separate the fill into multiple service calls: first update balance, then update holdings, then insert transaction record — each in its own transaction.

**Why it's wrong:** Any failure between steps leaves the account in a corrupt state (e.g., balance deducted but holdings not updated). In financial systems this is catastrophic and difficult to remediate.

**Do this instead:** Single `@Transactional` method that does all writes atomically. Any exception rolls back all changes. Use `SELECT ... FOR UPDATE` on the accounts row to prevent concurrent double-spend on the same account.

### Anti-Pattern 4: Calling External Price APIs on Every Order

**What people do:** Call KIS API or Alpha Vantage directly inside `executeFill()` to get the fill price.

**Why it's wrong:** External API calls add 200–2000ms latency and are subject to rate limits and failures. Order processing target is < 100ms.

**Do this instead:** Always read fill price from Redis cache. The cache is populated by MarketDataService on its own polling cycle. Decouple the two concerns.

### Anti-Pattern 5: Single Global Redis Key for Pending Orders

**What people do:** Store all pending orders in one Redis list or set, then scan it on every price tick.

**Why it's wrong:** Requires scanning all pending orders across all symbols for every tick, regardless of which symbol moved. O(N) scan on every tick is expensive.

**Do this instead:** Partition by symbol. Redis Sorted Sets keyed by symbol: `pending_buy:{symbol}` and `pending_sell:{symbol}`. ZRANGEBYSCORE only touches orders for the updated symbol.

---

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| KIS Open API (국내) | REST polling + optional WebSocket stream | Rate limits apply. Use Spring WebClient with retry/backoff. Domestic stocks only. API key required. |
| Alpha Vantage (해외) | REST polling | Free tier: 5 req/min, 500 req/day. Phase 1–3: poll on 15s interval, cache aggressively. Paid tier needed at scale. |
| Yahoo Finance (backup) | REST polling | No official API; use third-party wrapper. Fallback only when Alpha Vantage fails. |
| 한국은행 API (FX) | REST polling every 60s | USD/KRW rate. Cache in Redis key `fx:USD_KRW` with 60s TTL. |
| Google/Kakao/Apple OAuth | OAuth 2.0 Authorization Code flow | Spring Security OAuth2 Client handles most of this. Store `provider_id` + `provider` in users table. |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| MarketDataService → TradeService (pending fills) | Spring ApplicationEvent (in-process) or Redis Pub/Sub (multi-instance) | In Phase 1–3: in-process event. Phase 4+: Redis channel as instances scale. |
| TradeService → NotificationService (fill events) | Spring ApplicationEvent (async) | Notify user of fill after transaction commits. Use `@TransactionalEventListener(phase = AFTER_COMMIT)` to prevent notification on rollback. |
| Scheduler → TradeService (EOD cancel) | Direct service call within same JVM | Scheduler calls `cancelPendingOrders(beforeTime=marketClose)` |
| API Gateway → Backend (WebSocket) | Nginx `proxy_pass` with `Upgrade: websocket` header | Must configure `proxy_read_timeout` to 0 for long-lived connections. |

---

## Key Design Decisions (from SDD context)

| Decision | Rationale |
|----------|-----------|
| Simulated fill (not LOB engine) | 90% of simulation value, 10% of implementation complexity |
| MySQL for all mutable state | ACID guarantees are non-negotiable for financial data integrity |
| Redis for prices only (not state) | Redis is cache, MySQL is source of truth — never reverse this |
| REST polling in Phase 1–3 | Defer WebSocket complexity until core trading loop is proven |
| Average Cost for P&L (not FIFO) | Standard Korean retail brokerage method; simpler than FIFO lot tracking |
| `@TransactionalEventListener(AFTER_COMMIT)` for notifications | Prevents sending fill notification if the transaction rolls back |

---

## Sources

- [NautilusTrader execution architecture](https://docs.rs/nautilus-execution/latest/nautilus_execution/) — MEDIUM confidence: open-source paper trading reference
- [Alpaca Paper Trading docs](https://docs.alpaca.markets/docs/paper-trading) — HIGH confidence: production paper trading SaaS
- [Spring @Transactional best practices — Vlad Mihalcea](https://vladmihalcea.com/spring-transaction-best-practices/) — HIGH confidence: authoritative Spring/JPA source
- [Scaling WebSockets with Spring Boot — Medium](https://medium.com/@ShantKhayalian/scaling-millions-of-real-time-connections-with-spring-boot-websockets-a763bb47ee55) — MEDIUM confidence: community article
- [How Trading Platforms Handle Real-Time Data — Medium](https://medium.com/@anandjeyaseelan10/how-trading-platforms-handle-real-time-data-streaming-using-java-and-spring-boot-9803484c068b) — MEDIUM confidence: community article
- [Redis Sorted Sets documentation](https://redis.io/docs/latest/develop/data-types/sorted-sets/) — HIGH confidence: official Redis docs
- [Saxo: PnL FIFO Method](https://www.help.saxo/hc/en-us/articles/360041450651-How-do-I-calculate-PnL-FIFO-Method) — MEDIUM confidence: real broker documentation
- Project SDD (`docs/planning/sdd.md`) — HIGH confidence: primary source, project-defined schema and service boundaries

---

*Architecture research for: Paper Trading / Virtual Investment Platform (모의투자 플랫폼)*
*Researched: 2026-03-26*

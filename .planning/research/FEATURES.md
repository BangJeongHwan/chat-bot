# Feature Research

**Domain:** Paper Trading / Virtual Investment Platform (모의투자 플랫폼)
**Researched:** 2026-03-26
**Confidence:** HIGH (cross-referenced against SDD spec, competitor analysis, and multiple industry sources)

---

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete or untrustworthy.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Email + password registration | Every web service baseline | LOW | bcrypt hashing required; email verification flow adds Medium complexity |
| JWT-based authentication (Access + Refresh tokens) | Security standard for SPAs | LOW | Access 15min / Refresh 7d; token rotation on refresh call |
| Real-time (or near-real-time) current price display | Core simulation credibility — stale prices break trust | MEDIUM | v1: REST polling acceptable; Phase 4: WebSocket push |
| Market order (시장가) execution | Simplest and most-used order type | LOW | Immediate fill at current price; balance deduction must be atomic |
| Limit order (지정가) execution | Expected by any user who has traded before | MEDIUM | Requires pending order queue + price-check scheduler |
| Order history / trade log | Users need to review what they did | LOW | Paginated list: symbol, type, quantity, fill price, timestamp |
| Portfolio dashboard — total assets, P&L, return % | Primary feedback loop of the simulator | MEDIUM | Must reflect unrealized gains using live price; snapshot logic needed |
| Holdings list with average buy price | Standard in every brokerage UI | LOW | Computed from transactions; average cost basis recalculation on partial fills |
| Account balance display (KRW + USD) | Users need to know what they can trade | LOW | Separate KRW and USD balances for multi-market support |
| Stock search by name or symbol | Discovery is a prerequisite to trading | LOW | Korean (한글명), English, ticker code; full-text search or prefix match |
| Candlestick chart with multiple timeframes | Every trading platform has this | HIGH | 1분/5분/15분/1시간/일/주/월봉; requires chart data API (KIS, Alpha Vantage) |
| Order book / bid-ask display (호가창) | Expected from any Korean brokerage UI experience | MEDIUM | 10-level bid/ask depth; refreshed via polling or WebSocket |
| KOSPI + KOSDAQ support | Korean market coverage is non-negotiable for domestic users | MEDIUM | KIS Open API for real-time data; stock master table seeded at launch |
| NYSE + NASDAQ support | Modern investors expect US market access | HIGH | Alpha Vantage / Yahoo Finance; timezone and trading hours logic required |
| Trading hours enforcement | Simulation credibility — orders during market close should be rejected or queued | MEDIUM | Domestic 09:00–15:30 KST; US 09:30–16:00 ET with timezone conversion |
| Fee simulation (수수료) | Without fees, simulator teaches bad habits | LOW | 0.015%–0.5% range; deducted at fill time |
| Watchlist (관심목록) | Universal feature across all brokerages | LOW | Add/remove stocks; persist per user; live price refresh |
| Account reset (초기화) | Essential for a practice platform — users need fresh starts | LOW | Wipes holdings, orders, transactions; resets balance to initial amount |
| Basic stock detail page (PER, PBR, EPS, 시가총액) | Users need fundamental data before deciding | MEDIUM | Pulled from KIS/Alpha Vantage; cached per symbol |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required for launch, but create competitive moat.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Multiple virtual accounts (복수 계좌) | Strategy isolation — test momentum vs dividend strategy side-by-side | MEDIUM | Separate balances, holdings, P&L per account; account switcher in UI |
| Stop and Stop-Limit orders | Covers professional order vocabulary; competitors (Kiwoom 모의투자) often restrict to market+limit only | MEDIUM | Trigger-price monitoring loop; Stop fires market order, Stop-Limit fires limit order |
| GTC (Good-Till-Cancelled) orders | Advanced traders expect this; DAY-only is limiting | MEDIUM | Persist orders across sessions; auto-cancel logic for DAY orders at market close |
| Benchmark comparison chart (vs KOSPI / S&P 500) | Shows users whether they are beating the market — powerful motivator | MEDIUM | Index data feed required; overlaid on portfolio return graph |
| Risk metrics: Sharpe Ratio, MDD, Volatility | Differentiates from simple P&L platforms; appeals to strategy-minded users | HIGH | Requires daily portfolio snapshots; rolling calculation window |
| Sector / industry allocation breakdown | Portfolio quality insight beyond raw P&L | MEDIUM | Depends on sector field in stock master; pie/bar chart UI |
| FX rate application for US stocks (실시간 환율) | Korean users trading US stocks need KRW-denominated view | MEDIUM | BOK (한국은행) API for exchange rate; updated daily or on-demand |
| ETF detail page (구성 종목, 추적지수, 배당률) | ETF trading is growing; dedicated detail view builds trust | MEDIUM | ETF-specific data fields; separate stock type in stock master |
| Leaderboard — daily/weekly/monthly return ranking | Gamification drives retention; KRX Academy and school competitions use ranking | HIGH | Redis Sorted Set for ranked scores; privacy control (opt-in public ranking) |
| User level and badge system (레벨 / 뱃지) | Progression system sustains engagement beyond first week | HIGH | XP from trades, milestones, consecutive logins; badge definitions table |
| Multiple watchlist groups (반도체, 배당주 그룹) | Power users want organized monitoring | LOW | Group name + list of stocks; grouping in watchlist table |
| Transaction timeline view | Narrative of investment journey; unique among basic simulators | LOW | Ordered list of fills with dates; visual timeline optional enhancement |
| Win rate + realized/unrealized PnL per holding | Per-stock analytics; helps users learn from individual trades | MEDIUM | Computed from transaction history; unrealized from live price |
| Portfolio allocation chart (국내/해외/ETF/현금) | Asset class visibility at a glance | LOW | Computed from holdings + cash balance; pie chart UI |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems in v1.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Real-time WebSocket price push (v1) | Users want live ticking prices | WebSocket infra adds significant operational complexity before core trading is validated; connection management, scaling, and reconnect logic all need production hardening | REST polling every 3–10 seconds is sufficient for a practice platform; move to WebSocket in Phase 4 after user base validates need |
| Push / Email / In-App notification system (v1) | Users want order fill alerts and price alerts | Notification pipelines (FCM, APNS, SES/SendGrid) are orthogonal infrastructure with high setup cost; errors here distract from trading core | Build core trading first; notifications in Phase 4 after portfolio analytics prove retention |
| Social community / stock discussion forum | Feels engaging; platforms like StockTwits show demand | Moderation, content policy, spam prevention, and user reporting are a separate product surface; premature community features fragment the team focus | Focus on portfolio sharing (public/private toggle) in Phase 4; full forum is Phase 5+ |
| Margin / leverage simulation | Advanced users will ask for it | Margin math (margin call triggers, interest accrual) requires separate account model; risk of teaching dangerous leverage habits without proper guardrails | Keep virtual accounts as cash-only in v1; margin simulation is a v2+ feature with explicit warning UX |
| Algorithmic / automated trading (bots) | Tech-savvy users want strategy automation | Requires execution engine hooks, strategy DSL, backtesting infra — a separate product. Robinhood excluded this; thinkorswim ThinkScript required years of development | Offer portfolio analytics and historical trade review as the strategy feedback tool in v1 |
| Market replay / historical simulation | Advanced practice tool; thinkorswim "OnDemand" is popular | Requires full historical tick data storage — storage and API costs are high; feature complexity is disproportionate to v1 user needs | Provide chart history (1분–월봉 via KIS/Alpha Vantage) for manual analysis without replay mode |
| Options / derivatives trading | Users from thinkorswim background expect it | Options pricing (Black-Scholes Greeks), complex multi-leg order matching, and contract lifecycle are an entirely separate domain from equity simulation | Stay equities + ETF only in v1; derivatives can be Phase 6+ if user research validates demand |
| React Native mobile app (v1) | Users want mobile access | Mobile release requires App Store review cycles, device testing matrix, and push notification infrastructure before core web product is validated | Web-first with responsive design; share business logic (API types, state management hooks) in shared packages for Phase 5 React Native build |
| News feed integration (v1) | Context for investment decisions | RSS/news API reliability and licensing vary; news display without linking to actual decisions adds noise without analytical value | Add news in Phase 3+ as a supplementary view; not blocking for trading core |
| Broker research reports / analyst targets | Professional user expectation | External licensing cost, data freshness complexity, and parsing variability; no open/free source in Korea | Link to public sources (KIND, DART) rather than ingesting reports directly |

---

## Feature Dependencies

```
[User Authentication]
    └──requires──> [Account System]
                       └──requires──> [Order Execution Engine]
                                          └──requires──> [Real-Time Price Feed]
                                          └──requires──> [Balance Validation]
                                          └──requires──> [Fee Calculation]

[Real-Time Price Feed]
    └──requires──> [Stock Master Table (종목 마스터)]
    └──enhances──> [Order Book Display]
    └──enhances──> [Chart Data]

[Order Execution Engine]
    └──produces──> [Transaction History]
                       └──requires──> [Portfolio Dashboard]
                       └──requires──> [Holdings List]

[Holdings List]
    └──requires──> [Portfolio Dashboard]
    └──enhances──> [Sector Allocation Chart]
    └──enhances──> [Risk Metrics (Sharpe, MDD)]

[Portfolio Dashboard]
    └──enhances──> [Benchmark Comparison Chart]
    └──enhances──> [Risk Metrics (Sharpe, MDD)]

[Transaction History]
    └──enhances──> [Win Rate / Per-Holding PnL Analytics]
    └──enhances──> [Leaderboard] (via portfolio snapshot)

[Portfolio Snapshots (daily)]
    └──requires──> [Risk Metrics (Sharpe, MDD, Volatility)]
    └──requires──> [Leaderboard] (ranking by cumulative return)

[Leaderboard]
    └──enhances──> [User Level / Badge System]

[Watchlist]
    └──requires──> [Real-Time Price Feed]
    └──enhances──> [Notification System] (Phase 4)

[Multiple Virtual Accounts]
    └──requires──> [Account System]
    └──conflicts──> [Single-account leaderboard ranking] (must normalize by initial balance)
```

### Dependency Notes

- **Order Execution requires Real-Time Price Feed:** Market orders fill at current price; limit/stop orders require price monitoring. Price feed must be operational before order engine can be tested.
- **Risk Metrics require Portfolio Snapshots:** Sharpe ratio and MDD need a time series of portfolio values. Snapshot job must run before analytics can be computed.
- **Leaderboard requires Portfolio Snapshots:** Ranking by return requires consistent valuation at a point in time, not live prices (which vary second-to-second).
- **Multiple Accounts conflict with naive Leaderboard:** Ranking must use the best-performing account per user, or explicitly scope to one account per user per competition period.
- **FX Rate depends on Account System:** USD balance revaluation to KRW for total assets display requires exchange rate to be fetched at the same time as price data.

---

## MVP Definition

### Launch With (v1 — Phase 1–3)

Minimum viable product to validate simulation credibility and core trading loop.

- [ ] Email/password + OAuth registration and login — without auth, nothing else exists
- [ ] Default virtual account (1억 KRW) auto-created on signup — immediate value, no setup friction
- [ ] KOSPI/KOSDAQ stock master seeded — trading cannot start without universe
- [ ] NYSE/NASDAQ stock master seeded — multi-market is in core value proposition
- [ ] Real-time current price (REST polling) — simulation credibility depends on this
- [ ] Candlestick chart with standard timeframes — expected without exception
- [ ] Order book (호가창) 10-level bid/ask display — expected from Korean market users
- [ ] Market order + Limit order execution — 90%+ of retail trades use these two types
- [ ] Trading hours enforcement — credibility requirement; orders outside hours must be handled
- [ ] Fee simulation (0.015%–0.5%) — teaches realistic cost accounting
- [ ] Pending order management (view / modify / cancel) — without this, limit orders are a trap
- [ ] Portfolio dashboard (total assets, P&L, return %) — primary product feedback loop
- [ ] Holdings list with average buy price, unrealized P&L — per-position awareness
- [ ] Transaction history timeline — learning from past trades
- [ ] Stock search (Korean name, English name, ticker) — discoverability prerequisite
- [ ] Stock detail page (PER, PBR, EPS, market cap, sector) — decision-making context
- [ ] Watchlist (single group) — users need a "monitor later" list
- [ ] Account reset — practice platform must allow fresh starts
- [ ] FX rate application for USD-denominated stocks — multi-market integrity

### Add After Validation (v1.x — Phase 4)

Add once core trading is working and user retention is measurable.

- [ ] Multiple virtual accounts — trigger: users request strategy segmentation
- [ ] Stop and Stop-Limit orders — trigger: power user cohort engages with limit orders heavily
- [ ] GTC order support — trigger: limit/stop orders prove popular
- [ ] WebSocket real-time price push — trigger: polling latency causing user complaints
- [ ] Benchmark comparison (vs KOSPI / S&P 500) — trigger: portfolio dashboard DAU is healthy
- [ ] Sector / industry allocation chart — trigger: users exploring portfolio quality
- [ ] Risk metrics (Sharpe, MDD, Volatility) — trigger: strategy-minded user segment identified
- [ ] Win rate + per-holding PnL breakdown — trigger: advanced analytics tab gets clicks
- [ ] Portfolio snapshots (daily job) — prerequisite for risk metrics and leaderboard
- [ ] Leaderboard (daily/weekly/monthly) — trigger: community cohort forms
- [ ] User level + badge system — trigger: retention curve shows drop-off after first week
- [ ] Price alert / notification system (in-app) — trigger: watchlist engagement data shows need
- [ ] Multiple watchlist groups — trigger: power users managing >20 stocks in single list

### Future Consideration (v2+)

Defer until product-market fit with core trading is established.

- [ ] Community / stock discussion forum — defer until user base size justifies moderation cost
- [ ] React Native mobile app — defer until web flows are stable and tested
- [ ] News feed integration — defer until analytical value is validated by user behavior
- [ ] Market replay / historical simulation — defer; storage and API cost disproportionate to v1 users
- [ ] Algorithmic / automated trading — separate product surface; defer indefinitely unless research validates demand
- [ ] Options / derivatives simulation — separate domain; defer unless equity-only platform saturates
- [ ] Broker research reports (증권사 리포트) — licensing and data complexity; defer
- [ ] Margin / leverage simulation — requires separate risk model; defer with explicit safety design

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| User authentication (email + OAuth) | HIGH | LOW | P1 |
| Virtual account creation + balance | HIGH | LOW | P1 |
| Real-time price feed (REST polling) | HIGH | MEDIUM | P1 |
| Market + Limit order execution | HIGH | MEDIUM | P1 |
| Trading hours enforcement | HIGH | LOW | P1 |
| Portfolio dashboard (P&L, return) | HIGH | MEDIUM | P1 |
| Holdings list + average buy price | HIGH | LOW | P1 |
| Stock search | HIGH | LOW | P1 |
| Candlestick chart (multi-timeframe) | HIGH | HIGH | P1 |
| Order book (호가창) | HIGH | MEDIUM | P1 |
| KOSPI/KOSDAQ + NYSE/NASDAQ market coverage | HIGH | HIGH | P1 |
| Transaction history | MEDIUM | LOW | P1 |
| Watchlist (single group) | MEDIUM | LOW | P1 |
| Stock detail page (fundamentals) | MEDIUM | MEDIUM | P1 |
| Account reset | MEDIUM | LOW | P1 |
| FX rate for USD stocks | MEDIUM | MEDIUM | P1 |
| Fee simulation | LOW | LOW | P1 |
| Stop / Stop-Limit orders | MEDIUM | MEDIUM | P2 |
| GTC order support | MEDIUM | MEDIUM | P2 |
| Multiple virtual accounts | MEDIUM | MEDIUM | P2 |
| Benchmark comparison chart | HIGH | MEDIUM | P2 |
| Risk metrics (Sharpe, MDD) | MEDIUM | HIGH | P2 |
| Sector allocation analytics | MEDIUM | MEDIUM | P2 |
| Leaderboard | MEDIUM | HIGH | P2 |
| User level + badge system | MEDIUM | HIGH | P2 |
| WebSocket real-time push | MEDIUM | HIGH | P2 |
| Price alerts / notifications | MEDIUM | HIGH | P2 |
| Multiple watchlist groups | LOW | LOW | P2 |
| News feed | LOW | MEDIUM | P3 |
| Community forum | LOW | HIGH | P3 |
| Market replay | LOW | HIGH | P3 |
| React Native mobile app | HIGH | HIGH | P3 |
| Algorithmic trading | LOW | HIGH | P3 |
| Options / derivatives | LOW | HIGH | P3 |

**Priority key:**
- P1: Must have for launch (Phase 1–3)
- P2: Should have, add when core is validated (Phase 4)
- P3: Nice to have, future consideration (Phase 5+)

---

## Competitor Feature Analysis

| Feature | KRX 모의투자 (Game) | Kiwoom 모의투자 | thinkorswim paperMoney | Our Approach |
|---------|-------------------|----------------|------------------------|--------------|
| Markets covered | KOSPI/KOSDAQ only | KOSPI/KOSDAQ | US equities + options + futures | KOSPI/KOSDAQ + NYSE/NASDAQ + ETF |
| Order types | Market + Limit | Market + Limit | Full suite incl. options multi-leg | Market + Limit + Stop + Stop-Limit (GTC Phase 4) |
| Real-time price | Yes (during game) | Yes (limited) | Yes (tick-by-tick) | REST polling v1; WebSocket Phase 4 |
| Order book depth | Unknown | Yes (5-level typical) | Yes (Level 2 data) | 10-level bid/ask |
| Portfolio analytics | Basic P&L + ranking | Basic P&L | Advanced (Greeks, risk curves) | P&L + Sharpe + MDD + sector breakdown (Phase 2–4) |
| Benchmark comparison | Relative ranking only | None found | Against indexes | vs KOSPI + S&P500 (Phase 4) |
| Leaderboard / ranking | Yes — competition-style | None found | None | Phase 4 with daily/weekly/monthly periods |
| Gamification (badges, levels) | Competition medals | None found | None | Phase 4 XP + badge system |
| Multiple accounts | No | No | Yes (paper account per real account) | Yes — strategy segmentation |
| Account reset | Yes (per competition) | Yes | Yes | Yes — always available |
| FX / multi-currency | No | No | Yes | Yes — KRW + USD, BOK rate |
| Social / community | None | None | Community scripts (TradingView style) | Phase 5+ |
| Mobile app | No | Yes (MTS app) | Yes (thinkorswim mobile) | Phase 5 React Native |
| Fee simulation | Unknown | Yes | Yes | Yes (0.015%–0.5%) |

---

## Sources

- [StockBrokers.com — Best Paper Trading Platforms 2026](https://www.stockbrokers.com/guides/paper-trading)
- [ETNA — Why Advanced Simulation Sets the 2025 Standard](https://www.etnasoft.com/best-paper-trading-platform-for-u-s-broker-dealers-why-advanced-simulation-sets-the-2025-standard/)
- [ChartMini — thinkorswim Paper Trading Review & Alternatives 2026](https://chartmini.com/blog/thinkorswim-paper-trading-review-alternatives)
- [Benzinga — Best Paper Trading Options Platforms March 2026](https://www.benzinga.com/money/paper-trading-options)
- [한국투자증권 — 모의투자 안내](https://securities.koreainvestment.com/main/research/virtual/_static/TF07da010000.jsp)
- [KRX Academy — 신나는 경제게임 (모의투자 게임)](https://main.krxverse.co.kr/krx-academy/capital/game)
- [키움증권 OpenAPI GitHub — Kiwoom virtual account behavior](https://github.com/me2nuk/stockOpenAPI)
- [TradingView — Watchlist alerts and paper trading integration](https://www.tradingview.com/support/solutions/43000739708-watchlist-alerts-your-trading-edge/)
- [LuxAlgo — Top 5 Metrics for Evaluating Trading Strategies](https://www.luxalgo.com/blog/top-5-metrics-for-evaluating-trading-strategies/)
- [Berkeley Technology Law Journal — Gamification of Investments 2025](https://btlj.org/2025/11/the-gamification-of-investments-a-comparative-approach-between-the-us-and-eu/)
- [Gianty — Designing Leaderboards in Gamified Systems](https://www.gianty.com/leaderboard-gamified-systems-gamification/)
- [Kiwoom Open Trading API (Korea Investment)](https://github.com/koreainvestment/open-trading-api)
- SDD spec: `/Users/bangjeonghwan/IdeaProjects/project/stockbot/docs/planning/sdd.md`
- Project context: `/Users/bangjeonghwan/IdeaProjects/project/stockbot/.planning/PROJECT.md`

---
*Feature research for: Paper Trading / Virtual Investment Platform (모의투자 플랫폼)*
*Researched: 2026-03-26*

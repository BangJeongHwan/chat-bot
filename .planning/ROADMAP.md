# Roadmap: StockBot — 모의투자 플랫폼

## Overview

StockBot delivers a paper trading simulator where users practice investing with virtual money against real market prices. The build follows a strict dependency chain: the database schema and authentication layer must exist before any order can be placed, the order engine requires a live price cache before fills can execute, and the portfolio dashboard derives all its value from transaction records produced by the order engine. Three phases deliver the complete v1 experience — authenticated users trading real market-priced securities and reviewing their portfolio results.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Foundation and Authentication** - DB schema baseline, JWT auth, and default virtual account auto-creation
- [ ] **Phase 2: Market Data and Order Execution** - Real market price cache and atomic order fill engine (co-developed)
- [ ] **Phase 3: Portfolio Dashboard and Account Features** - P&L dashboard, FX rate application, and transaction history

## Phase Details

### Phase 1: Foundation and Authentication
**Goal**: Authenticated users exist and arrive with a funded virtual account ready to trade
**Depends on**: Nothing (first phase)
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, ACCT-01
**Success Criteria** (what must be TRUE):
  1. User can register with email and password and receives a verification email
  2. User can verify their email address via the link and their account becomes active
  3. User can log in and remain logged in across browser refreshes without re-entering credentials
  4. User can request a password reset via email and complete the reset via the emailed link
  5. A new account automatically has a KRW 100,000,000 virtual balance visible after login
**Plans**: TBD

### Phase 2: Market Data and Order Execution
**Goal**: Users can search for stocks, view live prices, and execute buy/sell orders that atomically update their balance and holdings
**Depends on**: Phase 1
**Requirements**: MKTD-01, MKTD-02, MKTD-03, MKTD-04, ORDR-01, ORDR-02, ORDR-03, ORDR-04, ORDR-05, ORDR-06, ORDR-07
**Success Criteria** (what must be TRUE):
  1. User can search for a domestic or international stock by Korean name, English name, or ticker symbol and see results
  2. User can view the current price, open, high, low, and volume for a stock, plus candlestick charts across all supported timeframes (1m/5m/15m/1h/daily/weekly/monthly)
  3. User can view a stock's fundamental data including company overview, PER, PBR, EPS, and market capitalization
  4. User can submit a market order or limit order and the order is rejected if trading hours are outside the valid window for that market
  5. A filled order correctly deducts the purchase amount plus fee from the user's balance and adds the shares to their holdings; a failed order leaves both unchanged
  6. User can view their list of pending (unfilled) orders and cancel individual orders; Day orders that are not filled by market close are automatically cancelled
**Plans**: TBD
**UI hint**: yes

### Phase 3: Portfolio Dashboard and Account Features
**Goal**: Users can review the results of their trading — total assets, unrealized P&L, transaction history — and account balances correctly reflect foreign exchange rates
**Depends on**: Phase 2
**Requirements**: PORT-01, PORT-02, PORT-03, PORT-04, ACCT-02, ACCT-03
**Success Criteria** (what must be TRUE):
  1. User can view a dashboard showing total assets, unrealized P&L (in KRW), and return percentage, updated to reflect current market prices
  2. User can view a pie chart showing their portfolio allocation by individual holding
  3. User can view a performance chart comparing their portfolio returns by day/week/month against KOSPI and S&P 500 benchmarks
  4. User can view a timeline of all their executed trades (buy/sell, symbol, quantity, price, fee, timestamp)
  5. User's KRW balance and total asset value correctly incorporate real-time FX conversion of USD-denominated holdings
  6. User can view their KRW and USD balances separately on the account page
**Plans**: TBD
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation and Authentication | 1/2 | In Progress|  |
| 2. Market Data and Order Execution | 0/? | Not started | - |
| 3. Portfolio Dashboard and Account Features | 0/? | Not started | - |

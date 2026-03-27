# 모의투자 서비스 설계 문서

> **Version 1.0** | 2026년 3월 | 플랫폼: Web + Mobile | 대상: 국내/해외 주식, ETF

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기능 명세](#2-기능-명세)
3. [ERD (Database Schema)](#3-erd-database-schema)
4. [테이블 관계도](#4-테이블-관계도)
5. [API 설계](#5-api-설계)
6. [API Request/Response 예시](#6-api-requestresponse-예시)
7. [시스템 아키텍처](#7-시스템-아키텍처)
8. [비기능 요구사항](#8-비기능-요구사항)
9. [개발 로드맵](#9-개발-로드맵)
10. [부록](#10-부록)

---

## 1. 프로젝트 개요

### 1.1 서비스 소개

본 서비스는 실제 자금 없이 가상의 자금으로 국내/해외 주식 및 ETF에 투자 경험을 제공하는 모의투자 플랫폼입니다. 초보 투자자가 안전하게 투자 전략을 연습하고, 경험자는 새로운 전략을 테스트할 수 있습니다.

### 1.2 플랫폼 및 기술 스택

| 구분 | 기술 | 선택 이유 |
|------|------|-----------|
| **Frontend (Web)** | React + TypeScript + TailwindCSS | 실시간 시세처럼 빈번한 상태 변경 UI에 강한 Virtual DOM 기반 리렌더링. TypeScript로 금액/수량 타입 안전성 확보. TailwindCSS로 반응형 레이아웃 빠른 구현 |
| **Frontend (Mobile)** | React Native (iOS/Android) | Web React와 상태관리/비즈니스 로직 공유 가능. 단일 코드베이스로 iOS+Android 커버. 네이티브 성능에 근접한 결과물 |
| **Backend** | Kotlin + Spring Boot | 코루틴 기반 비동기 처리로 외부 시세 API 호출에 유리. Null Safety로 금액 계산 시 NPE 방지. Spring 트랜잭션 관리로 주문 체결 데이터 정합성 보장 |
| **Database (Main)** | MySQL 8.0 | ACID 트랜잭션으로 주문/체결/잔고의 원자적 처리 보장. 외래 키로 데이터 정합성 확보. Kotlin + JPA/Hibernate와의 궁합 우수 |
| **Cache / Session** | Redis | 실시간 시세 캐싱(수백ms 단위 갱신), 세션 관리, 리더보드 랭킹(Sorted Set)에 최적 |
| **Real-time** | WebSocket (Socket.IO) | 실시간 시세 및 체결 알림을 서버 푸시 방식으로 효율적 전달 |
| **시세 데이터** | KIS Open API, Alpha Vantage, Yahoo Finance | 국내(KIS), 해외(Alpha Vantage) 시세 수집. Yahoo Finance를 백업용으로 활용 |
| **Infra** | AWS / GCP + Docker + Kubernetes | 컨테이너 기반 배포로 수평 확장 용이 |

### 1.3 Web / Mobile 분리 이유

Web과 Mobile은 **API 호출 로직, 타입 정의, 유틸리티 함수**를 공유 패키지로 분리하여 중복을 최소화하되, UI 레이어는 분리합니다.

- **Web**: 넓은 화면에 차트 + 호가창 + 주문창을 동시 배치하는 멀티패널 레이아웃
- **Mobile**: 한 화면에 핵심 정보 하나를 보여주는 탭/스택 네비게이션 + Push 알림(FCM/APNs), 생체 인증, 제스처 등 네이티브 기능 활용

### 1.4 핵심 목표

- 실제 시장 데이터 기반의 사실적인 모의투자 환경 제공
- 국내 주식(KOSPI/KOSDAQ), 해외 주식(NYSE/NASDAQ), ETF 지원
- 포트폴리오 분석, 수익률 추적, 리더보드 등 게임화 요소
- Web + Mobile 크로스플랫폼 지원으로 언제 어디서나 접근 가능
- 주문 체결 시뮬레이션(Market/Limit/Stop 주문 처리)

---

## 2. 기능 명세

### 2.1 사용자 관리

#### 2.1.1 회원가입 / 로그인

- 이메일 + 비밀번호 회원가입 (bcrypt 해싱)
- OAuth 2.0 소셜 로그인 (Google, Kakao, Apple)
- JWT 기반 인증 (Access Token + Refresh Token)
- 이메일 인증 / 비밀번호 찾기 / 비밀번호 변경

#### 2.1.2 프로필 관리

- 닉네임, 프로필 이미지, 투자 성향 설정
- 투자 스타일 태그 (보수적/중립/공격적)
- 알림 설정 (Push, Email, SMS)

### 2.2 계좌 시스템

#### 2.2.1 모의 계좌

- 회원가입 시 기본 가상 자금 지급 (ex: 1억원)
- 복수 계좌 지원 (전략별 분리 운용 가능)
- 계좌 초기화 (Reset) 기능
- 원화(KRW) 및 외화(USD) 잔고 분리 관리
- 환율 적용: 해외 주식 거래 시 실시간 환율 반영

### 2.3 시세 / 종목 정보

#### 2.3.1 시세 데이터

- 실시간 현재가, 시가, 고가, 저가, 거래량 조회
- 차트 데이터: 1분/5분/15분/1시간/일/주/월봉
- 호가창 (매수/매도 호가 10단계)
- 종목 검색 (한글명, 영문명, 종목코드)

#### 2.3.2 종목 상세 정보

- 기업 개요, 재무제표 (PER, PBR, EPS, 시가총액 등)
- ETF: 구성 종목, 운용보수료, 추적지수, 배당률
- 관련 뉴스 연동 (RSS 또는 뉴스 API)
- 증권사 리서치 보고서 요약 (목표가, 투자의견)

### 2.4 주문 / 체결 시스템

#### 2.4.1 주문 유형

| 주문 유형 | 설명 | 체결 방식 |
|-----------|------|-----------|
| 시장가 (Market) | 현재 시장 가격으로 즉시 체결 | 주문 즉시 현재가로 체결 처리 |
| 지정가 (Limit) | 지정한 가격 이하/이상 도달 시 체결 | 호가 도달 시 체결 처리 |
| Stop (손절매) | 지정 가격 도달 시 시장가 주문 발동 | Trigger 후 시장가로 전환 체결 |
| Stop-Limit | Trigger 가격 도달 시 지정가 주문 발동 | Trigger 후 지정가로 체결 |

#### 2.4.2 체결 엔진 로직

- 거래 시간 검증: 국내(09:00~15:30), 해외(NYSE 09:30~16:00 ET)
- 잔고 검증: 매수 시 가용 잔고 확인, 매도 시 보유 수량 확인
- 수수료 시뮬레이션: 실제 증권사 수수료율 반영 (0.015%~0.5%)
- 미체결 주문 관리: 주문 수정/취소, 미체결 목록 조회
- 장 마감 시 미체결 주문 자동 취소 (GTC 옵션 제외)

### 2.5 포트폴리오 / 분석

#### 2.5.1 포트폴리오 대시보드

- 총 자산 / 평가 손익 / 평가 수익률
- 보유 종목별 비중 파이 차트
- 일별/주별/월별 수익률 그래프 (vs KOSPI/S&P500 벤치마크 비교)
- 자산 배분 현황 (국내/해외/ETF/현금)
- 거래 내역 타임라인

#### 2.5.2 투자 분석 툴

- 종목별 평균 매입가, 실현 손익, 미실현 손익
- 섹터/산업별 포트폴리오 분석
- 위험 지표: 변동성, 샤프비, MDD(Maximum Drawdown)
- 승률 / 평균 수익률 / 평균 손실률 통계

### 2.6 소셜 / 게임화

#### 2.6.1 리더보드

- 전체 수익률 랭킹 (일간/주간/월간/누적)
- 종목별 투자 성과 랭킹
- 프로필 뱃지 / 레벨 시스템
- 초보/중급/고급 등급 구분

#### 2.6.2 커뮤니티 기능

- 종목 토론방 (종목 단위 채팅)
- 투자 전략 공유 (포트폴리오 공개/비공개)
- 팔로잉 / 팔로워 시스템
- 투자 성과 공유 스냅샷 생성

### 2.7 알림 시스템

- 체결 완료 알림 (Push + In-App)
- 목표가 도달 알림 (상한/하한 설정)
- 급등/급락 알림 (설정 %)
- 리더보드 랭킹 변동 알림
- 마켓 개장/마감 알림

### 2.8 관심목록 / 워치리스트

- 종목 관심목록 등록/삭제
- 복수 관심 그룹 생성 (예: 반도체, 배당주 등)
- 관심목록 실시간 시세 모니터링
- 알림 연동 (관심종목 급등/급락 시 알림)

---

## 3. ERD (Database Schema)

> 모든 테이블은 `created_at`, `updated_at` 타임스탬프를 기본 포함합니다.
> MySQL 8.0 기준 스키마이며, UUID는 `CHAR(36)` 또는 `BINARY(16)`으로 저장합니다.

### 3.1 users

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 사용자 고유 ID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 이메일 주소 |
| password_hash | VARCHAR(255) | NOT NULL | bcrypt 해시 비밀번호 |
| nickname | VARCHAR(50) | UNIQUE | 닉네임 |
| profile_image_url | VARCHAR(500) | NULLABLE | 프로필 이미지 URL |
| provider | VARCHAR(20) | DEFAULT 'local' | OAuth 프로바이더 |
| provider_id | VARCHAR(255) | NULLABLE | 소셜 로그인 ID |
| invest_style | VARCHAR(20) | NULLABLE | 투자 성향 (보수/중립/공격) |
| level | INT | DEFAULT 1 | 사용자 레벨 |
| experience | INT | DEFAULT 0 | 경험치 |
| is_active | TINYINT(1) | DEFAULT 1 | 활성 상태 |

### 3.2 accounts (모의 계좌)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 계좌 고유 ID |
| user_id | CHAR(36) | FK → users.id | 소유자 |
| name | VARCHAR(100) | NOT NULL | 계좌 이름 (ex: 전략 A) |
| balance_krw | DECIMAL(18,2) | DEFAULT 100000000 | 원화 잔고 (1억) |
| balance_usd | DECIMAL(18,4) | DEFAULT 0 | 달러 잔고 |
| initial_balance | DECIMAL(18,2) | NOT NULL | 초기 자금 |
| is_default | TINYINT(1) | DEFAULT 0 | 기본 계좌 여부 |

### 3.3 stocks (종목 마스터)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 종목 고유 ID |
| symbol | VARCHAR(20) | UNIQUE, NOT NULL | 종목 코드 (005930, AAPL) |
| name_ko | VARCHAR(100) | NOT NULL | 한글 종목명 |
| name_en | VARCHAR(100) | NULLABLE | 영문 종목명 |
| market | VARCHAR(20) | NOT NULL | KOSPI/KOSDAQ/NYSE/NASDAQ |
| type | VARCHAR(10) | NOT NULL | STOCK / ETF |
| sector | VARCHAR(50) | NULLABLE | 섹터/업종 |
| currency | VARCHAR(3) | NOT NULL | KRW / USD |
| is_active | TINYINT(1) | DEFAULT 1 | 거래 가능 여부 |

### 3.4 orders (주문)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 주문 고유 ID |
| account_id | CHAR(36) | FK → accounts.id | 계좌 |
| stock_id | CHAR(36) | FK → stocks.id | 종목 |
| type | VARCHAR(10) | NOT NULL | BUY / SELL |
| order_type | VARCHAR(15) | NOT NULL | MARKET/LIMIT/STOP/STOP_LIMIT |
| quantity | INT | NOT NULL | 주문 수량 |
| price | DECIMAL(18,4) | NULLABLE | 지정가 (시장가일 경우 NULL) |
| stop_price | DECIMAL(18,4) | NULLABLE | Stop 트리거 가격 |
| status | VARCHAR(15) | NOT NULL | PENDING/FILLED/PARTIAL/CANCELLED |
| filled_quantity | INT | DEFAULT 0 | 체결된 수량 |
| filled_price | DECIMAL(18,4) | NULLABLE | 평균 체결 가격 |
| fee | DECIMAL(18,4) | DEFAULT 0 | 수수료 |
| time_in_force | VARCHAR(5) | DEFAULT 'DAY' | DAY / GTC |
| ordered_at | DATETIME | NOT NULL | 주문 시각 |
| filled_at | DATETIME | NULLABLE | 체결 시각 |

### 3.5 holdings (보유 종목)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 보유 고유 ID |
| account_id | CHAR(36) | FK → accounts.id | 계좌 |
| stock_id | CHAR(36) | FK → stocks.id | 종목 |
| quantity | INT | NOT NULL | 보유 수량 |
| avg_buy_price | DECIMAL(18,4) | NOT NULL | 평균 매입 단가 |
| total_invested | DECIMAL(18,4) | NOT NULL | 총 투자 금액 |

### 3.6 transactions (거래 내역)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 거래 고유 ID |
| order_id | CHAR(36) | FK → orders.id | 관련 주문 |
| account_id | CHAR(36) | FK → accounts.id | 계좌 |
| stock_id | CHAR(36) | FK → stocks.id | 종목 |
| type | VARCHAR(10) | NOT NULL | BUY / SELL |
| quantity | INT | NOT NULL | 체결 수량 |
| price | DECIMAL(18,4) | NOT NULL | 체결 가격 |
| fee | DECIMAL(18,4) | NOT NULL | 수수료 |
| realized_pnl | DECIMAL(18,4) | NULLABLE | 실현 손익 (SELL시) |
| executed_at | DATETIME | NOT NULL | 체결 시각 |

### 3.7 watchlists (관심목록)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 관심목록 고유 ID |
| user_id | CHAR(36) | FK → users.id | 사용자 |
| name | VARCHAR(50) | NOT NULL | 그룹명 (ex: 반도체) |
| sort_order | INT | DEFAULT 0 | 정렬 순서 |

### 3.8 watchlist_items (관심목록 종목)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 항목 고유 ID |
| watchlist_id | CHAR(36) | FK → watchlists.id | 관심목록 |
| stock_id | CHAR(36) | FK → stocks.id | 종목 |
| sort_order | INT | DEFAULT 0 | 정렬 순서 |

### 3.9 portfolio_snapshots (포트폴리오 스냅샷)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 스냅샷 고유 ID |
| account_id | CHAR(36) | FK → accounts.id | 계좌 |
| total_value | DECIMAL(18,2) | NOT NULL | 총 자산 평가가치 |
| cash_balance | DECIMAL(18,2) | NOT NULL | 현금 잔고 |
| invested_value | DECIMAL(18,2) | NOT NULL | 투자 금액 |
| pnl | DECIMAL(18,2) | NOT NULL | 손익금 |
| pnl_rate | DECIMAL(8,4) | NOT NULL | 수익률 (%) |
| snapshot_date | DATE | NOT NULL | 스냅샷 일자 |

### 3.10 notifications (알림)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 알림 고유 ID |
| user_id | CHAR(36) | FK → users.id | 사용자 |
| type | VARCHAR(30) | NOT NULL | ORDER_FILLED/PRICE_ALERT/RANK 등 |
| title | VARCHAR(200) | NOT NULL | 알림 제목 |
| message | TEXT | NOT NULL | 알림 내용 |
| is_read | TINYINT(1) | DEFAULT 0 | 읽음 여부 |
| data | JSON | NULLABLE | 추가 데이터 (stock_id, order_id 등) |

### 3.11 leaderboard (리더보드)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 리더보드 고유 ID |
| user_id | CHAR(36) | FK → users.id | 사용자 |
| account_id | CHAR(36) | FK → accounts.id | 계좌 |
| period | VARCHAR(10) | NOT NULL | DAILY/WEEKLY/MONTHLY/ALL |
| pnl_rate | DECIMAL(10,4) | NOT NULL | 수익률 (%) |
| rank_position | INT | NOT NULL | 랭킹 |
| period_date | DATE | NOT NULL | 기준 일자 |

### 3.12 price_alerts (가격 알림 설정)

| Column | Type | Constraint | Description |
|--------|------|------------|-------------|
| id | CHAR(36) | PK | 알림 고유 ID |
| user_id | CHAR(36) | FK → users.id | 사용자 |
| stock_id | CHAR(36) | FK → stocks.id | 종목 |
| alert_condition | VARCHAR(10) | NOT NULL | ABOVE / BELOW |
| target_price | DECIMAL(18,4) | NOT NULL | 목표 가격 |
| is_triggered | TINYINT(1) | DEFAULT 0 | 발동 여부 |
| is_active | TINYINT(1) | DEFAULT 1 | 활성 여부 |

---

## 4. 테이블 관계도

```
users (1) ──── (N) accounts
  │                    │
  │                    ├── (N) orders ──── (N) transactions
  │                    │
  │                    ├── (N) holdings
  │                    │
  │                    └── (N) portfolio_snapshots
  │
  ├── (N) watchlists ──── (N) watchlist_items ──── stocks
  │
  ├── (N) notifications
  │
  ├── (N) price_alerts ──── stocks
  │
  └── (N) leaderboard

stocks (1) ──── (N) orders
       (1) ──── (N) holdings
       (1) ──── (N) transactions
```

**관계 설명**: users는 복수의 accounts를 보유하며, 각 account는 독립된 orders, holdings, transactions, portfolio_snapshots를 가집니다. stocks 테이블은 종목 마스터 데이터로 orders, holdings, transactions에서 참조됩니다.

**주요 인덱스**:
- `orders`: `(account_id, status)`, `(stock_id, ordered_at)`, `(status, ordered_at)`
- `transactions`: `(account_id, executed_at)`, `(stock_id, executed_at)`
- `holdings`: `(account_id, stock_id)` UNIQUE
- `portfolio_snapshots`: `(account_id, snapshot_date)` UNIQUE
- `leaderboard`: `(period, period_date, rank_position)`

---

## 5. API 설계

> Base URL: `/api/v1` | 인증: Bearer Token (JWT) | 응답 형식: JSON

### 5.1 인증 (Auth)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/auth/register` | 회원가입 | - |
| POST | `/auth/login` | 로그인 (JWT 발급) | - |
| POST | `/auth/refresh` | Access Token 갱신 | Refresh |
| POST | `/auth/oauth/{provider}` | 소셜 로그인 | - |
| POST | `/auth/password/reset` | 비밀번호 재설정 요청 | - |
| PUT | `/auth/password` | 비밀번호 변경 | Bearer |

### 5.2 사용자 (Users)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/users/me` | 내 프로필 조회 | Bearer |
| PUT | `/users/me` | 프로필 수정 | Bearer |
| GET | `/users/{id}/profile` | 타인 프로필 조회 | Bearer |
| POST | `/users/{id}/follow` | 팔로우 | Bearer |
| DELETE | `/users/{id}/follow` | 언팔로우 | Bearer |

### 5.3 계좌 (Accounts)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/accounts` | 내 계좌 목록 조회 | Bearer |
| POST | `/accounts` | 새 모의계좌 생성 | Bearer |
| GET | `/accounts/{id}` | 계좌 상세 조회 | Bearer |
| POST | `/accounts/{id}/reset` | 계좌 초기화 | Bearer |
| DELETE | `/accounts/{id}` | 계좌 삭제 | Bearer |

### 5.4 종목/시세 (Stocks)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/stocks/search?q={keyword}` | 종목 검색 (한글/영문/코드) | Bearer |
| GET | `/stocks/{symbol}` | 종목 상세 정보 | Bearer |
| GET | `/stocks/{symbol}/price` | 현재 시세 조회 | Bearer |
| GET | `/stocks/{symbol}/chart?interval={1m\|5m\|1d}` | 차트 데이터 조회 | Bearer |
| GET | `/stocks/{symbol}/orderbook` | 호가창 조회 | Bearer |
| GET | `/stocks/{symbol}/news` | 관련 뉴스 조회 | Bearer |
| GET | `/stocks/market/{market}/top` | 시장별 상위 종목 | Bearer |

### 5.5 주문 (Orders)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/accounts/{id}/orders` | 주문 접수 (매수/매도) | Bearer |
| GET | `/accounts/{id}/orders` | 주문 목록 조회 | Bearer |
| GET | `/accounts/{id}/orders/pending` | 미체결 주문 목록 | Bearer |
| PUT | `/accounts/{id}/orders/{orderId}` | 주문 수정 | Bearer |
| DELETE | `/accounts/{id}/orders/{orderId}` | 주문 취소 | Bearer |

### 5.6 포트폴리오 (Portfolio)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/accounts/{id}/portfolio` | 포트폴리오 요약 (총자산/손익) | Bearer |
| GET | `/accounts/{id}/holdings` | 보유 종목 목록 | Bearer |
| GET | `/accounts/{id}/transactions` | 거래 내역 조회 | Bearer |
| GET | `/accounts/{id}/performance?period={1d\|1w\|1m\|3m\|1y}` | 수익률 그래프 데이터 | Bearer |
| GET | `/accounts/{id}/analysis` | 투자 분석 (섹터/위험지표) | Bearer |

### 5.7 관심목록 (Watchlist)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/watchlists` | 관심목록 그룹 목록 | Bearer |
| POST | `/watchlists` | 관심목록 그룹 생성 | Bearer |
| POST | `/watchlists/{id}/items` | 종목 추가 | Bearer |
| DELETE | `/watchlists/{id}/items/{stockId}` | 종목 삭제 | Bearer |
| DELETE | `/watchlists/{id}` | 그룹 삭제 | Bearer |

### 5.8 알림 (Notifications)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/notifications` | 알림 목록 조회 | Bearer |
| PUT | `/notifications/{id}/read` | 알림 읽음 처리 | Bearer |
| PUT | `/notifications/read-all` | 전체 읽음 처리 | Bearer |
| POST | `/price-alerts` | 가격 알림 설정 | Bearer |
| DELETE | `/price-alerts/{id}` | 가격 알림 삭제 | Bearer |

### 5.9 리더보드 (Leaderboard)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/leaderboard?period={daily\|weekly\|monthly\|all}` | 수익률 랭킹 조회 | Bearer |
| GET | `/leaderboard/me` | 내 랭킹 조회 | Bearer |

### 5.10 WebSocket 이벤트

| Event | Direction | Description |
|-------|-----------|-------------|
| `subscribe:price` | Client → Server | 실시간 시세 구독 |
| `price:update` | Server → Client | 시세 변동 데이터 푸시 |
| `subscribe:orderbook` | Client → Server | 호가창 실시간 구독 |
| `orderbook:update` | Server → Client | 호가창 변동 데이터 푸시 |
| `order:filled` | Server → Client | 주문 체결 알림 |
| `notification` | Server → Client | 실시간 알림 푸시 |

---

## 6. API Request/Response 예시

### 6.1 주문 접수 — POST `/accounts/{id}/orders`

**Request Body:**
```json
{
  "stock_id": "550e8400-e29b-41d4-a716-446655440000",
  "type": "BUY",
  "order_type": "LIMIT",
  "quantity": 10,
  "price": 72000,
  "time_in_force": "DAY"
}
```

**Response (201 Created):**
```json
{
  "id": "order-uuid-here",
  "stock": {
    "symbol": "005930",
    "name": "삼성전자"
  },
  "type": "BUY",
  "order_type": "LIMIT",
  "quantity": 10,
  "price": 72000,
  "status": "PENDING",
  "fee": 108,
  "ordered_at": "2026-03-25T10:30:00Z"
}
```

### 6.2 포트폴리오 요약 — GET `/accounts/{id}/portfolio`

**Response (200 OK):**
```json
{
  "total_value": 103520000,
  "cash_balance_krw": 28520000,
  "cash_balance_usd": 5420.50,
  "invested_value": 75000000,
  "unrealized_pnl": 3520000,
  "unrealized_pnl_rate": 4.69,
  "realized_pnl": 1200000,
  "total_pnl_rate": 3.52,
  "holdings_count": 8,
  "today_pnl": 520000,
  "today_pnl_rate": 0.50
}
```

### 6.3 에러 응답 형식

```json
{
  "error": {
    "code": "INSUFFICIENT_BALANCE",
    "message": "매수 가능 금액이 부족합니다.",
    "details": {
      "required": 720000,
      "available": 500000
    }
  }
}
```

---

## 7. 시스템 아키텍처

### 7.1 전체 구성도

```
┌──────────────────────────────────────────────┐
│              Client Layer                     │
│  ┌────────────┐  ┌─────────────────────────┐ │
│  │ React Web  │  │ React Native (iOS/AOS)  │ │
│  └────────────┘  └─────────────────────────┘ │
└─────────────────────┬────────────────────────┘
                      │ REST API + WebSocket
┌─────────────────────┼────────────────────────┐
│          API Gateway (Nginx)                  │
│         Rate Limiting / SSL Termination       │
└─────────────────────┬────────────────────────┘
                      │
┌─────────────────────┼────────────────────────┐
│         Backend (Kotlin + Spring Boot)        │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐  │
│  │ Auth Svc  │ │ Trade Svc │ │ Market    │  │
│  │           │ │ (Engine)  │ │ Data Svc  │  │
│  └───────────┘ └───────────┘ └───────────┘  │
│  ┌───────────┐ ┌───────────┐                 │
│  │Notify Svc │ │ Scheduler │                 │
│  └───────────┘ └───────────┘                 │
└─────────────────────┬────────────────────────┘
                      │
┌─────────────────────┼────────────────────────┐
│              Data Layer                       │
│  ┌───────────┐ ┌───────┐ ┌─────────────────┐│
│  │  MySQL    │ │ Redis │ │ External        ││
│  │ (Main DB) │ │(Cache)│ │ Market APIs     ││
│  └───────────┘ └───────┘ └─────────────────┘│
└──────────────────────────────────────────────┘
```

### 7.2 핵심 서비스 설명

| 서비스 | 역할 |
|--------|------|
| **Auth Service** | 회원가입, 로그인, JWT 발급/갱신, OAuth 연동 |
| **Trade Service** | 주문 접수/체결/취소, 잔고 검증, 수수료 계산, 포트폴리오 관리 |
| **Market Data Service** | 외부 API 연동, 실시간 시세 수집, WebSocket 브로드캐스팅, 차트 데이터 캐싱 |
| **Notification Service** | Push/Email/In-App 알림 발송, 가격 알림 모니터링, FCM/APNs 연동 |
| **Scheduler** | 장 마감 미체결 처리, 일일 스냅샷 생성, 리더보드 갱신 |

---

## 8. 비기능 요구사항

### 8.1 성능 목표

| 항목 | 목표 |
|------|------|
| API 응답 시간 | 95th percentile < 200ms |
| WebSocket 시세 지연 | < 500ms (External API 수신 ~ 클라이언트 전달) |
| 동시 접속자 수 | 10,000+ concurrent connections |
| 주문 처리 속도 | < 100ms per order |
| 가용성 | 99.9% uptime (거래 시간 기준) |

### 8.2 보안 요구사항

- 비밀번호 bcrypt 해싱 (salt rounds: 12)
- JWT Access Token 만료: 15분, Refresh Token: 7일
- HTTPS 전용 통신 (TLS 1.3)
- Rate Limiting: 인증 API 10회/분, 주문 API 60회/분
- SQL Injection / XSS 방어 (Input Validation + JPA)
- CORS 설정: 허용된 origin만 접근 가능

### 8.3 확장성

- Stateless 서버 설계로 Horizontal Scaling 지원
- Database: Read Replica 구성으로 읽기 성능 확보
- Redis Cluster로 캐시 고가용성 보장
- Message Queue (RabbitMQ/Kafka)로 비동기 처리 확장 가능

---

## 9. 개발 로드맵

| 단계 | 기간 | 주요 기능 |
|------|------|-----------|
| **Phase 1** | 1~4주차 | 회원가입/로그인, 모의계좌, 종목검색, 시세조회, DB 세팅 |
| **Phase 2** | 5~8주차 | 주문/체결 엔진, 보유종목 관리, 거래내역, 호가창 |
| **Phase 3** | 9~12주차 | 포트폴리오 분석, 차트, 수익률 그래프, 관심목록 |
| **Phase 4** | 13~16주차 | 실시간 WebSocket, 알림, 리더보드, 커뮤니티 |
| **Phase 5** | 17~20주차 | Mobile 앱 개발, 성능 최적화, QA, 베타 출시 |

---

## 10. 부록

### 10.1 용어 정의

| 용어 | 설명 |
|------|------|
| PER | Price Earnings Ratio — 주가수익비율 |
| PBR | Price Book-value Ratio — 주가순자산비율 |
| MDD | Maximum Drawdown — 최대낙폭. 고점 대비 최대 하락율 |
| GTC | Good Till Cancelled — 취소할 때까지 유효한 주문 |
| ETF | Exchange Traded Fund — 상장지수펀드 |
| KIS | Korea Investment & Securities — 한국투자증권 Open API |
| FCM | Firebase Cloud Messaging — 모바일 Push 알림 서비스 |

### 10.2 참고 외부 API

| API | 용도 | 비고 |
|-----|------|------|
| KIS Open API | 국내 시세 | 한국투자증권 제공, 실시간 + 호가 |
| Alpha Vantage | 해외 시세 | US 주식/ETF 시세, 무료 티어 있음 |
| Yahoo Finance | 보조 시세 | 백업용 시세 데이터 + 기업 정보 |
| 한국은행 API | 환율 | 실시간 환율 정보 (USD/KRW) |

---

*— End of Document —*
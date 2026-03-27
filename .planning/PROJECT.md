# 모의투자 서비스 (StockBot)

## What This Is

실제 자금 없이 가상의 자금으로 국내/해외 주식 및 ETF에 투자 경험을 제공하는 모의투자 플랫폼이다. 초보 투자자가 안전하게 투자 전략을 연습하고, 경험자는 새로운 전략을 테스트할 수 있다. Web 우선으로 개발하며, 핵심 거래 기능(인증·계좌·시세·주문·포트폴리오)을 Phase 1~3에서 완성한다.

## Core Value

실제 시장 데이터 기반의 사실적인 주문 체결 시뮬레이션 — 이것이 동작하지 않으면 나머지는 의미 없다.

## Requirements

### Validated

(None yet — ship to validate)

### Active

#### 인증 / 사용자
- [ ] 이메일 + 비밀번호 회원가입 (bcrypt 해싱)
- [ ] JWT 기반 로그인 (Access Token 15분 / Refresh Token 7일)
- [ ] OAuth 소셜 로그인 (Google, Kakao, Apple)
- [ ] 이메일 인증 / 비밀번호 찾기 / 비밀번호 변경
- [ ] 프로필 관리 (닉네임, 프로필 이미지, 투자 성향 태그)

#### 계좌 시스템
- [ ] 회원가입 시 기본 가상 자금 1억원 자동 지급
- [ ] 복수 모의계좌 생성 및 관리 (전략별 분리 운용)
- [ ] 계좌 초기화(Reset) 기능
- [ ] 원화(KRW) 및 달러(USD) 잔고 분리 관리
- [ ] 해외 주식 거래 시 실시간 환율 반영

#### 시세 / 종목 정보
- [ ] 실시간 현재가, 시가, 고가, 저가, 거래량 조회
- [ ] 차트 데이터 (1분/5분/15분/1시간/일/주/월봉)
- [ ] 호가창 (매수/매도 10단계)
- [ ] 종목 검색 (한글명, 영문명, 종목코드)
- [ ] 종목 상세 정보 (기업 개요, 재무지표 PER/PBR/EPS/시가총액)
- [ ] ETF 정보 (구성 종목, 운용보수, 추적지수, 배당률)
- [ ] 국내(KOSPI/KOSDAQ) + 해외(NYSE/NASDAQ) + ETF 지원

#### 주문 / 체결 엔진
- [ ] 시장가(Market) / 지정가(Limit) / Stop / Stop-Limit 주문 지원
- [ ] 거래 시간 검증 (국내 09:00~15:30, 해외 NYSE 09:30~16:00 ET)
- [ ] 잔고 검증 (매수: 가용 잔고 / 매도: 보유 수량)
- [ ] 수수료 시뮬레이션 (0.015%~0.5%)
- [ ] 미체결 주문 조회 / 수정 / 취소
- [ ] 장 마감 시 미체결 주문 자동 취소 (GTC 제외)

#### 포트폴리오 / 분석
- [ ] 총 자산 / 평가 손익 / 평가 수익률 대시보드
- [ ] 보유 종목별 비중 파이 차트
- [ ] 일별/주별/월별 수익률 그래프 (vs KOSPI/S&P500 벤치마크)
- [ ] 자산 배분 현황 (국내/해외/ETF/현금)
- [ ] 거래 내역 타임라인
- [ ] 종목별 평균 매입가, 실현 손익, 미실현 손익
- [ ] 섹터/산업별 포트폴리오 분석
- [ ] 위험 지표 (변동성, 샤프비, MDD)

#### 관심목록 (Watchlist)
- [ ] 종목 관심목록 등록/삭제
- [ ] 복수 관심 그룹 생성 (예: 반도체, 배당주)
- [ ] 관심목록 실시간 시세 모니터링

### Out of Scope

- **실시간 WebSocket 시세 푸시** — Phase 4 범위. v1은 REST polling으로 시세 제공
- **알림 시스템 (Push/Email/In-App)** — Phase 4 범위. 핵심 거래 기능 완성 후 진행
- **리더보드 / 게임화** — Phase 4 범위. 사용자 기반 확보 후 의미 있음
- **커뮤니티 기능** (종목 토론방, 투자 전략 공유, 팔로잉) — Phase 4 범위
- **React Native Mobile 앱** — Phase 5 범위. Web 완성 후 로직 공유하여 개발
- **증권사 리서치 보고서 요약** — 외부 데이터 의존성, 추후 고려
- **뉴스 연동** — 초기 범위 외, Phase 3 이후 추가 검토

## Context

- **기술 스택 확정**: Kotlin + Spring Boot (백엔드), React + TypeScript + TailwindCSS (Web), MySQL 8.0, Redis, WebSocket
- **시세 데이터 소스**: KIS Open API (국내), Alpha Vantage (해외), Yahoo Finance (백업), 한국은행 API (환율)
- **인프라**: AWS/GCP + Docker + Kubernetes, Nginx API Gateway
- **설계 문서**: `docs/planning/sdd.md`에 ERD, API 설계, 아키텍처 다이어그램이 상세히 정의되어 있음
- **DB 스키마**: users, accounts, stocks, orders, holdings, transactions, watchlists, portfolio_snapshots 등 12개 테이블 설계 완료
- **성능 목표**: API 응답 95th percentile < 200ms, 주문 처리 < 100ms, 동시 접속 10,000+

## Constraints

- **Tech Stack**: Kotlin + Spring Boot 백엔드 — 팀 결정 사항, 변경 불가
- **Tech Stack**: React + TypeScript 프론트엔드 — 이후 React Native와 로직 공유 목적
- **Database**: MySQL 8.0 — ACID 트랜잭션 필수 (주문/체결 정합성)
- **Security**: JWT Access Token 15분, Refresh Token 7일, bcrypt salt 12 — 금융 서비스 보안 기준
- **Market Data**: 외부 API 의존성 (KIS, Alpha Vantage) — API 키 관리 및 Rate Limit 주의
- **Scope**: Phase 1~3만 구현 — Web 우선, Mobile은 Phase 5에서 진행

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Web 우선 개발 | 멀티패널 레이아웃(차트+호가창+주문창)은 Web이 적합. React Native는 Web 완성 후 로직 공유 | — Pending |
| Phase 1~3 범위 설정 | 핵심 거래 기능(인증/계좌/시세/주문/포트폴리오) 완성이 우선. 소셜/알림/모바일은 이후 | — Pending |
| REST polling (v1 시세) | WebSocket은 Phase 4. 초기엔 REST로 빠르게 검증 | — Pending |
| 복수 계좌 지원 | 전략별 분리 운용 허용 → 사용자 실험 다양성 증가 | — Pending |

---

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd:transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-03-26 after initialization*

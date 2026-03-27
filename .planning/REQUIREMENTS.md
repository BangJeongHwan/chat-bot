# Requirements: StockBot — 모의투자 플랫폼

**Defined:** 2026-03-26
**Core Value:** 실제 시장 데이터 기반의 사실적인 주문 체결 시뮬레이션 — 이것이 동작하지 않으면 나머지는 의미 없다.

## v1 Requirements

### Authentication (AUTH)

- [ ] **AUTH-01**: 사용자가 이메일과 비밀번호로 회원가입할 수 있다 (bcrypt salt 12)
- [ ] **AUTH-02**: 회원가입 후 이메일 인증 링크를 받을 수 있다
- [ ] **AUTH-03**: 사용자가 이메일 링크로 비밀번호를 재설정할 수 있다
- [ ] **AUTH-04**: 사용자 세션이 브라우저 새로고침 후에도 유지된다 (JWT Refresh Token)

### Account (ACCT)

- [ ] **ACCT-01**: 회원가입 시 원화 잔고 1억원의 기본 모의계좌가 자동 생성된다
- [ ] **ACCT-02**: 사용자가 계좌의 원화(KRW)와 달러(USD) 잔고를 별도로 조회할 수 있다
- [ ] **ACCT-03**: 해외 주식 거래 시 실시간 환율이 원화 환산 총자산에 반영된다

### Market Data (MKTD)

- [ ] **MKTD-01**: 사용자가 종목 코드/한글명/영문명으로 종목을 검색할 수 있다 (국내 + 해외)
- [ ] **MKTD-02**: 사용자가 종목의 현재가, 시가, 고가, 저가, 거래량을 조회할 수 있다
- [ ] **MKTD-03**: 사용자가 1분/5분/15분/1시간/일/주/월봉 차트 데이터를 조회할 수 있다
- [ ] **MKTD-04**: 사용자가 종목의 기업 개요 및 재무지표(PER, PBR, EPS, 시가총액)를 조회할 수 있다

### Order Execution (ORDR)

- [ ] **ORDR-01**: 사용자가 시장가(Market) 매수/매도 주문을 제출할 수 있다
- [ ] **ORDR-02**: 사용자가 지정가(Limit) 매수/매도 주문을 제출할 수 있다
- [ ] **ORDR-03**: 주문 제출 시 가용 잔고 및 보유 수량이 검증된다
- [ ] **ORDR-04**: 거래 시간이 검증된다 (국내 09:00~15:30 KST, NYSE 09:30~16:00 ET / DST 반영)
- [ ] **ORDR-05**: 수수료가 시뮬레이션된다 (0.015%~0.5% 범위)
- [ ] **ORDR-06**: 사용자가 미체결 주문 목록을 조회하고 취소할 수 있다
- [ ] **ORDR-07**: 장 마감 시 DAY 주문이 자동 취소된다

### Portfolio (PORT)

- [ ] **PORT-01**: 사용자가 총 자산, 평가 손익, 평가 수익률을 대시보드에서 확인할 수 있다
- [ ] **PORT-02**: 사용자가 보유 종목별 비중을 파이 차트로 확인할 수 있다
- [ ] **PORT-03**: 사용자가 일별/주별/월별 수익률 그래프를 KOSPI/S&P500 벤치마크와 비교할 수 있다
- [ ] **PORT-04**: 사용자가 거래 내역 타임라인을 조회할 수 있다

## v2 Requirements

### Account — Deferred

- **ACCT-V2-01**: 사용자가 전략별 복수 모의계좌를 생성하고 관리할 수 있다
- **ACCT-V2-02**: 사용자가 계좌를 초기화(Reset)할 수 있다

### Auth — Deferred

- **AUTH-V2-01**: 사용자가 Google, Kakao, Apple OAuth 2.0으로 로그인할 수 있다
- **AUTH-V2-02**: 사용자가 닉네임, 프로필 이미지, 투자 성향 태그를 관리할 수 있다

### Market Data — Deferred

- **MKTD-V2-01**: 사용자가 매수/매도 10단계 호가창을 실시간으로 확인할 수 있다
- **MKTD-V2-02**: ETF 구성 종목, 운용보수, 추적지수, 배당률을 조회할 수 있다

### Order — Deferred

- **ORDR-V2-01**: 사용자가 Stop / Stop-Limit 주문을 제출할 수 있다
- **ORDR-V2-02**: 사용자가 GTC(Good Till Cancelled) 주문을 설정할 수 있다

### Portfolio Analytics — Deferred

- **PORT-V2-01**: 섹터/산업별 포트폴리오 분석을 확인할 수 있다
- **PORT-V2-02**: 변동성, 샤프비, MDD(Maximum Drawdown) 위험 지표를 확인할 수 있다
- **PORT-V2-03**: 종목별 평균 매입가, 실현/미실현 손익을 상세히 조회할 수 있다

### Watchlist — Deferred

- **WTCH-V2-01**: 사용자가 종목 관심목록을 그룹별로 관리할 수 있다
- **WTCH-V2-02**: 관심목록 종목의 실시간 시세를 모니터링할 수 있다

### Social & Gamification — Deferred (Phase 4+)

- **SOCL-01**: 전체/일간/주간/월간 수익률 리더보드
- **SOCL-02**: 레벨/뱃지 시스템
- **SOCL-03**: 투자 전략 공유 및 팔로잉

### Notifications — Deferred (Phase 4)

- **NOTF-01**: 주문 체결 완료 알림 (Push + In-App)
- **NOTF-02**: 목표가 도달 알림 (상한/하한 설정)
- **NOTF-03**: 급등/급락 알림

## Out of Scope

| Feature | Reason |
|---------|--------|
| WebSocket 실시간 시세 푸시 | Phase 4 범위. v1은 REST polling (1초 주기). 조기 복잡도 방지 |
| Socket.IO | Spring 미지원. Phase 4에서 Spring STOMP WebSocket으로 구현 |
| React Native 모바일 앱 | Phase 5. Web 완성 후 로직 공유 개발. Web 우선 검증이 우선 |
| 알림 시스템 (Push/Email) | Phase 4. 핵심 거래 기능 완성 후 의미 있음 |
| 커뮤니티 기능 (토론방, 팔로잉) | Phase 4. 사용자 기반 확보 후 진행 |
| 리더보드 | Phase 4. 포트폴리오 스냅샷 기반이 필요 (Phase 3 이후) |
| 증권사 리서치 보고서 요약 | 외부 데이터 의존성 높음, 추후 고려 |
| 뉴스 연동 | 초기 범위 외. Phase 3 이후 추가 검토 |
| 알고리즘 트레이딩 | 별도 프로덕트 수준의 복잡도 |
| 옵션/선물/파생상품 | 현물 주식 시뮬레이션만 지원 |

## Traceability

*(Populated during roadmap creation)*

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | — | Pending |
| AUTH-02 | — | Pending |
| AUTH-03 | — | Pending |
| AUTH-04 | — | Pending |
| ACCT-01 | — | Pending |
| ACCT-02 | — | Pending |
| ACCT-03 | — | Pending |
| MKTD-01 | — | Pending |
| MKTD-02 | — | Pending |
| MKTD-03 | — | Pending |
| MKTD-04 | — | Pending |
| ORDR-01 | — | Pending |
| ORDR-02 | — | Pending |
| ORDR-03 | — | Pending |
| ORDR-04 | — | Pending |
| ORDR-05 | — | Pending |
| ORDR-06 | — | Pending |
| ORDR-07 | — | Pending |
| PORT-01 | — | Pending |
| PORT-02 | — | Pending |
| PORT-03 | — | Pending |
| PORT-04 | — | Pending |

**Coverage:**
- v1 requirements: 22 total
- Mapped to phases: 0 (roadmap 생성 후 업데이트)
- Unmapped: 22 ⚠️

---
*Requirements defined: 2026-03-26*
*Last updated: 2026-03-26 after initial definition*

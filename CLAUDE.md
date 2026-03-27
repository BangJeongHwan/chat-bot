<!-- GSD:project-start source:PROJECT.md -->
## Project

**모의투자 서비스 (StockBot)**

실제 자금 없이 가상의 자금으로 국내/해외 주식 및 ETF에 투자 경험을 제공하는 모의투자 플랫폼이다. 초보 투자자가 안전하게 투자 전략을 연습하고, 경험자는 새로운 전략을 테스트할 수 있다. Web 우선으로 개발하며, 핵심 거래 기능(인증·계좌·시세·주문·포트폴리오)을 Phase 1~3에서 완성한다.

**Core Value:** 실제 시장 데이터 기반의 사실적인 주문 체결 시뮬레이션 — 이것이 동작하지 않으면 나머지는 의미 없다.

### Constraints

- **Tech Stack**: Kotlin + Spring Boot 백엔드 — 팀 결정 사항, 변경 불가
- **Tech Stack**: React + TypeScript 프론트엔드 — 이후 React Native와 로직 공유 목적
- **Database**: MySQL 8.0 — ACID 트랜잭션 필수 (주문/체결 정합성)
- **Security**: JWT Access Token 15분, Refresh Token 7일, bcrypt salt 12 — 금융 서비스 보안 기준
- **Market Data**: 외부 API 의존성 (KIS, Alpha Vantage) — API 키 관리 및 Rate Limit 주의
- **Scope**: Phase 1~3만 구현 — Web 우선, Mobile은 Phase 5에서 진행
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## Recommended Stack
### Core Technologies
| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | 2.2.x (stable as of June 2025) | Backend language | Null safety eliminates NPE on financial calculations. Coroutines allow non-blocking I/O for external market-data API calls without the cognitive overhead of reactive streams. Team-decided; aligns with Spring Boot idioms. |
| Spring Boot | **3.5.13** | Backend framework | Latest patch on the 3.5.x line (released 2026-03-26). Spring Boot 3.4 reached EOL December 2025. Spring Boot 4.0 requires Jackson 3.x, JUnit 6, and Jakarta EE 11 — too many simultaneous migration risks for a greenfield project in March 2026. Use 3.5.x; migrate to 4.0 after stabilization (~2026 H2). |
| Spring Framework | 6.2.x (bundled with Boot 3.5) | DI, MVC, WebSocket | Bundled with Spring Boot 3.5. Do not declare independently. |
| Spring Security | 6.4.x (bundled) | Auth, JWT filter chain | `SecurityFilterChain` bean replaces `WebSecurityConfigurerAdapter` (removed in 6.0). Native OAuth2 Resource Server support handles JWT validation without custom filters. |
| Java (JVM) | **21 LTS** | JVM target for Kotlin | Spring Boot 3.5 minimum is Java 17; Java 21 is the current LTS with virtual threads (Project Loom) GA. Virtual threads pair well with Kotlin coroutines and will ease a future Boot 4.0 migration. |
| MySQL | **8.0.x** | Primary relational store | ACID transactions guarantee order/fill/balance atomicity. `DECIMAL(18,4)` columns map to Kotlin `BigDecimal` without precision loss. Locked by project constraints. |
| Redis | **7.x** | Cache + session + pubsub | Real-time price caching (sub-second TTL). Sorted Sets for leaderboard rankings. Spring Session Redis for horizontal scaling. Lettuce (default in `spring-boot-starter-data-redis`) is preferred over Jedis — thread-safe, non-blocking, Netty-backed. |
| React | **19.2.x** | Frontend SPA framework | React 19 stable (Dec 2024); 19.2.x (Oct 2025) adds stable Server Components and finer concurrency control. Virtual DOM efficiently handles the high-frequency re-renders required for a real-time quote ticker. |
| TypeScript | **5.7.x** | Frontend type safety | Enforces types on price/quantity/currency fields at compile time, preventing class of bugs common in financial UIs. Ships with Vite react-ts template. |
| TailwindCSS | **4.x** | Utility-first CSS | v4 released January 2025 with CSS-first config (no `tailwind.config.js`), 5x faster full build, first-party Vite plugin. Locked by project constraints. No breaking changes for pure utility class usage. |
| Vite | **8.x** | Frontend build tool | Vite 8 released March 2026 with Rolldown (Rust bundler), 10-30x faster builds vs Vite 6/7. The `npm create vite@latest --template react-swc-ts` scaffold installs all required plugins. CRA is deprecated; do not use. |
### Supporting Libraries — Backend
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `spring-boot-starter-web` | (Boot-managed) | REST API | All REST controllers, JSON serialization via Jackson |
| `spring-boot-starter-websocket` + STOMP | (Boot-managed) | Real-time push (Phase 4+) | STOMP over WebSocket for fan-out price subscription topics. Use `@EnableWebSocketMessageBroker` with in-memory broker for Phase 4; swap to RabbitMQ/Redis broker for 10k+ concurrent users in Phase 5. Native WebSocket (no STOMP) is appropriate only for simple 1:1 connections; STOMP is required for pub-sub price feeds and order notifications. |
| `spring-boot-starter-data-jpa` | (Boot-managed) | ORM / repositories | All entity persistence. Use `@Transactional` on service methods that touch `orders`, `holdings`, and `accounts` together. |
| `spring-boot-starter-data-redis` | (Boot-managed) | Redis integration | Uses Lettuce by default — do not override to Jedis. |
| `spring-boot-starter-security` | (Boot-managed) | Security filter chain | JWT stateless auth, OAuth2 Social login |
| `spring-boot-starter-oauth2-resource-server` | (Boot-managed) | JWT validation | Built-in `JwtDecoder` / `BearerTokenAuthenticationFilter`. Replaces manual JWT filter boilerplate. |
| `io.jsonwebtoken:jjwt-api` | **0.13.0** | JWT creation (access + refresh tokens) | Use the modular split: `jjwt-api` (compile), `jjwt-impl` + `jjwt-jackson` (runtime only). Latest release August 2025. The legacy single-artifact `jjwt` is deprecated. |
| `io.jsonwebtoken:jjwt-impl` | **0.13.0** | JWT implementation | runtime scope only |
| `io.jsonwebtoken:jjwt-jackson` | **0.13.0** | JWT Jackson serializer | runtime scope only |
| `com.fasterxml.jackson.module:jackson-module-kotlin` | (Boot-managed) | Kotlin data class JSON | Required for Jackson to correctly serialize/deserialize Kotlin data classes and nullable fields |
| `org.springframework.boot:spring-boot-starter-validation` | (Boot-managed) | Bean Validation (JSR-380) | Validate incoming order requests (`@NotNull`, `@Positive` on quantity/price) |
| `org.springframework.boot:spring-boot-starter-mail` | (Boot-managed) | Email delivery | Email verification, password reset |
| `com.github.line:kotlin-jdsl-spring-data-jpa-support` | **3.x** | Type-safe JPA queries | Preferred over QueryDSL for Kotlin — no annotation processor, no Q-class generation. Use for complex dynamic queries (order history filters, portfolio analytics). QueryDSL still works but requires Jakarta-updated kapt config; JDSL is simpler for pure Kotlin codebases. |
| `io.micrometer:micrometer-registry-prometheus` | (Boot-managed) | Metrics export | Exposes `/actuator/prometheus` for Prometheus/Grafana. Required to hit the 95th-percentile < 200ms SLA target. |
| `org.springframework.boot:spring-boot-starter-actuator` | (Boot-managed) | Health checks + metrics | `/actuator/health` for K8s liveness/readiness probes |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | (Boot-managed via Boot) | Java 8 date/time JSON | Serialize `LocalDate`, `ZonedDateTime` correctly for market hours and transaction timestamps |
### Supporting Libraries — Frontend
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `@tanstack/react-query` | **5.x** | Server state / data fetching | The standard for async server state in React 2025. Handles caching, background refetch, stale-while-revalidate for price polling (Phase 1-3). Replaces manually written useEffect + fetch + loading state patterns. `gcTime` (renamed from `cacheTime` in v5) — do not confuse. |
| `zustand` | **5.x** | UI / client-side state | For local state that doesn't come from the server: selected account, active order form, sidebar open/close, WebSocket connection status. Use TanStack Query for server state; Zustand for UI state. Redux Toolkit is overkill for this scope. |
| `react-hook-form` | **7.x** | Form state + validation | Order entry form (price, quantity, order type) and registration forms. Pairs with `zod` for schema validation. Avoids uncontrolled re-renders on every keystroke. |
| `zod` | **3.x** | Schema validation | Validate order form input on the client before sending. Share schema definitions with TypeScript types via `z.infer`. |
| `axios` | **1.x** | HTTP client | Interceptors for attaching `Authorization: Bearer` headers and auto-refreshing tokens on 401. TanStack Query's `queryFn` wraps axios calls. |
| `lightweight-charts` | **4.x** (TradingView) | Candlestick / OHLC charts | Canvas-based; handles 5,000+ data points without DOM thrashing. Recharts (SVG-based) freezes at this density. Use for 1m/5m/1h/daily candlestick and volume bar charts. Use Recharts only for low-density portfolio analytics charts (pnl curve, pie chart). |
| `recharts` | **2.x** | Portfolio analytics charts | SVG-based; excellent React integration, good for pie charts (asset allocation), line charts (daily P&L), and bar charts with < 500 points. Do NOT use for OHLC price charts. |
| `@stomp/stompjs` | **7.x** | WebSocket / STOMP client | Phase 4+ real-time price subscriptions. Works directly against Spring's STOMP WebSocket broker. SockJS fallback available but not required for modern browsers. |
| `date-fns` | **3.x** | Date manipulation | Formatting trade timestamps, computing market open/close times by timezone (`America/New_York` for NYSE). Lighter than `moment.js` (do not use moment.js — unmaintained). |
| `react-router-dom` | **7.x** | Client-side routing | Navigate between dashboard, stock detail, portfolio, order history pages |
### Development Tools
| Tool | Purpose | Notes |
|------|---------|-------|
| Gradle (Kotlin DSL) | Backend build | Use `build.gradle.kts`, not Groovy `build.gradle`. Kotlin DSL provides type-safe build scripts; consistent with a Kotlin codebase. |
| `ktlint` | Kotlin linter | Enforce consistent formatting. Use `com.pinterest.ktlint` Gradle plugin. |
| Flyway | DB schema migrations | `spring-boot-starter-data-jpa` + Flyway auto-runs `V{n}__{description}.sql` on startup. Never run DDL manually in production. |
| `testcontainers-mysql` | Integration test DB | Spin up real MySQL 8.0 in Docker during `@SpringBootTest`. Avoid H2 — H2 behavior diverges from MySQL on DECIMAL arithmetic and datetime handling, which will mask bugs in the order engine. |
| `testcontainers-redis` | Integration test cache | Real Redis in test containers for session/cache integration tests |
| MockK | Kotlin mock library | Preferred over Mockito for Kotlin — handles `data class`, `object`, and `suspend fun` correctly. |
| Spring RestAssured / MockMvc | Controller tests | Use `MockMvc` for slice tests (`@WebMvcTest`). Use RestAssured for full integration tests. |
| ESLint + `typescript-eslint` | Frontend linter | Catch type-unsafe patterns and React hooks violations at dev time |
| Vitest | Frontend unit tests | Vite-native test runner; zero-config with `react-swc-ts` scaffold. Faster than Jest for Vite projects. |
| React Testing Library | Component tests | Test component behavior, not implementation. Pairs with Vitest. |
| Docker Compose | Local dev environment | Single `docker-compose.yml` with MySQL 8.0, Redis 7, and optional Prometheus/Grafana. |
## Installation
### Backend (`build.gradle.kts` key dependencies)
### Frontend
# Scaffold (Vite 8 + React 19 + TypeScript + SWC)
# Tailwind v4 (Vite plugin — no tailwind.config.js)
# Server state + HTTP
# Client state + forms
# Routing
# Charts
# WebSocket (Phase 4)
# Utilities
# Dev dependencies
## Alternatives Considered
| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Spring Boot 3.5.13 | Spring Boot 4.0.x | When the project's dependencies (Jackson 3.x, JUnit 6, Jakarta EE 11) have all stabilized and team can afford a focused migration sprint. Spring Boot 4.0 requires Java 17+ minimum; strongly prefer Java 21+ for its virtual threads. Not recommended for greenfield projects in March 2026 until the ecosystem (third-party libraries) has caught up. |
| Kotlin JDSL | QueryDSL | QueryDSL is acceptable if the team already knows it. Requires kapt (annotation processor) configuration and Q-class generation. For pure Kotlin codebases, JDSL reduces build tooling complexity. |
| TanStack Query v5 | SWR | SWR is simpler but lacks the mutation lifecycle management critical for order submission flows (optimistic updates, rollback on error). TanStack Query's `useMutation` + `invalidateQueries` pattern fits this domain well. |
| Zustand | Redux Toolkit | Redux Toolkit is the right choice if the team grows large (5+ frontend engineers) and needs strict action-based state traceability. For a startup-phase project, Zustand's lower boilerplate and faster iteration wins. |
| Lightweight Charts (TradingView) | Recharts for all charts | Recharts SVG rendering freezes at ~1,000 data points. Use Recharts only for portfolio analytics (pie, line with daily granularity). Use Lightweight Charts for all OHLC price charts regardless of timeframe. |
| Vite 8 | webpack / CRA | CRA is deprecated (no longer maintained). webpack requires manual optimization. Vite 8 with Rolldown provides the best DX and build performance in 2026. |
| Java 21 LTS | Java 17 LTS | Java 17 is the Boot 3.5 minimum and remains valid. Use Java 21 to get virtual threads (Project Loom) for Tomcat thread pool improvements and to reduce migration delta when upgrading to Boot 4.0 later. |
| MySQL 8.0 | PostgreSQL | PostgreSQL has superior JSON support and advisory locks. For this project MySQL 8.0 was team-decided and provides adequate ACID guarantees for the order engine. Not worth overriding. |
| STOMP over WebSocket | Raw WebSocket | Raw WebSocket is appropriate for simple 1:1 connections (e.g., single-user order status). STOMP is necessary for the fan-out price feed subscription model where 10,000+ clients subscribe to the same ticker topics. |
| Lettuce (default) | Jedis | Jedis is only preferable for simple scripting or single-threaded batch jobs. Lettuce is non-blocking and thread-safe; always preferred in a Spring Boot async context. |
| Flyway | Liquibase | Both are valid. Flyway's SQL-first approach is simpler, and the MySQL dialect is mature. Use Liquibase only if multi-database portability is required. |
| MockK | Mockito-Kotlin | Mockito-Kotlin is a wrapper that still has edge cases with Kotlin idioms (data classes, `object`, coroutines). MockK is built for Kotlin from the ground up. |
## What NOT to Use
| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Spring Boot 3.4.x or earlier | Spring Boot 3.4 reached EOL December 2025. No more open-source bug fixes or security patches. | Spring Boot 3.5.13 |
| `WebSecurityConfigurerAdapter` | Removed in Spring Security 6.0. Any tutorials using it are outdated. | `SecurityFilterChain` bean in a `@Configuration` class |
| H2 for integration tests | H2 dialect diverges from MySQL 8.0 on DECIMAL arithmetic, datetime functions, and window functions. Bugs in the order execution engine will be masked. | `testcontainers-mysql` with real MySQL 8.0 |
| Create React App (CRA) | Officially deprecated, no longer maintained by the React team. Produces much slower builds than Vite. | Vite 8 with `react-swc-ts` template |
| `moment.js` | Unmaintained (enters legacy mode). Large bundle size (67KB gzipped). | `date-fns` (tree-shakeable, actively maintained) |
| Legacy `jjwt` single-artifact | Marked deprecated in the jwtk/jjwt repo. Only `jjwt-api` + `jjwt-impl` + `jjwt-jackson` are maintained. | `io.jsonwebtoken:jjwt-api:0.13.0` (modular split) |
| Socket.IO (mentioned in SDD) | Socket.IO is a Node.js library. Spring does not have a first-class Socket.IO server implementation. Socket.IO clients expect a Socket.IO-specific handshake that Spring's WebSocket support does not provide natively. Using Socket.IO here would require a separate Node.js gateway — unnecessary complexity. | Spring's STOMP over WebSocket (`@EnableWebSocketMessageBroker`) with `@stomp/stompjs` on the frontend |
| `javax.persistence.*` imports | Removed in Spring Boot 3.x (Jakarta EE migration). Any library or tutorial using `javax.persistence` is pre-Boot-3 and incompatible. | `jakarta.persistence.*` imports (auto-used when Spring Boot 3 BOM is applied) |
| `Double` / `Float` for monetary values | Floating-point arithmetic introduces rounding errors in price and P&L calculations. | `BigDecimal` in Kotlin, `DECIMAL(18,4)` in MySQL. Map with `@Column(precision=18, scale=4)`. |
| `spring-boot-starter-webflux` + R2DBC | Spring WebFlux requires switching the entire stack (R2DBC for MySQL, reactive Redis, reactive security). This is a high-complexity choice that removes the ability to use JDBC/JPA, Hibernate, and the familiar Spring MVC programming model. For this project's complexity level, the coroutine support in Spring MVC achieves sufficient non-blocking I/O without the reactive stack's steep learning curve. | Stay on `spring-boot-starter-web` (servlet/MVC) + Kotlin coroutines for async external API calls |
| Alpha Vantage free tier for production | Free tier is limited to 25 requests/day (confirmed 2025). This is insufficient even for development integration testing. | Purchase Alpha Vantage premium ($25/month minimum for 30 req/min), or use Finnhub free tier (60 req/min) as primary US stock data source during development. Yahoo Finance as backup fallback. KIS Open API for Korean stocks (WebSocket available for real-time). |
## Stack Patterns by Variant
- Backend: Spring Boot MVC + JPA + Redis (price cache with 1s TTL)
- Frontend: Vite + React 19 + TanStack Query with 1-second `refetchInterval` for price polling
- No WebSocket dependencies needed yet
- Because: WebSocket is explicitly deferred to Phase 4 in PROJECT.md
- Backend: Add `spring-boot-starter-websocket` + `@EnableWebSocketMessageBroker` with in-memory STOMP broker
- Backend: Add Spring scheduler to push price updates from Redis to subscribed WebSocket topics
- Frontend: Add `@stomp/stompjs` client, replace TanStack Query polling with WebSocket subscription for real-time tickers
- Because: In-memory STOMP broker is sufficient up to ~3,000 concurrent connections; upgrade to Redis/RabbitMQ STOMP broker at Phase 5
- Replace in-memory STOMP broker with Redis Pub/Sub relay (Spring `RedisMessageListenerContainer`)
- Add read replicas for MySQL (reporting queries separate from transactional writes)
- Consider Kotlin coroutines + `suspend fun` on repository layer to reduce thread pool pressure
- Because: In-memory broker does not support multi-instance horizontal scaling
## Version Compatibility
| Package | Compatible With | Notes |
|---------|-----------------|-------|
| Spring Boot 3.5.13 | Kotlin 2.2.x, Java 17-21, Spring Security 6.4.x | Boot 3.5 BOM manages Spring Security, Spring Framework, Hibernate 6.6.x, Jackson 2.18.x versions. Do not override these unless there is a specific bug. |
| Spring Boot 3.5.13 | Flyway 10.x | Flyway 10 dropped support for MySQL 5.7. Use `flyway-mysql` artifact alongside `flyway-core`. Flyway 9.x and older are incompatible with Spring Boot 3.5. |
| Kotlin 2.2.x | `jackson-module-kotlin` 2.18.x | Must have `jackson-module-kotlin` on the classpath or Jackson will fail to deserialize Kotlin data classes with default values. Spring Boot BOM includes correct version. |
| `jjwt 0.13.0` | Spring Security 6.x | jjwt 0.13.0 uses Java 17 baseline, compatible with Spring Boot 3.5. The resource server JWT validator (`spring-boot-starter-oauth2-resource-server`) can be used alongside jjwt for the refresh token flow without conflict. |
| React 19.2.x | TailwindCSS 4.x, Vite 8 | TailwindCSS 4 first-party Vite plugin works with Vite 8's Rolldown architecture. Known minor PostCSS conflict with React 19 resolved with `@tailwindcss/vite` direct integration (no PostCSS needed). |
| Lightweight Charts 4.x | React 19.x | Lightweight Charts is a vanilla JS library; use the official React wrapper or wrap imperatively with `useRef`. No known React 19 incompatibilities. |
| TanStack Query v5 | React 19.x | TanStack Query v5 fully supports React 19 concurrent features and `use()` hook integration. |
| Kotlin JDSL 3.x | Spring Data JPA (Hibernate 6.6.x) | JDSL 3.x targets Jakarta persistence; compatible with Hibernate 6. JDSL 2.x used `javax.persistence` — do not use JDSL 2.x. |
## External API Integration Notes
### KIS Open API (한국투자증권)
- REST for historical and current price data, WebSocket for real-time streaming
- Authentication: `appkey` + `appsecret` → access token (REST), real-time key (WebSocket)
- **TLS 1.0/1.1 will be dropped after 2025-12-12.** Java 21 defaults to TLS 1.3 — no action required.
- Rate limits: Not publicly documented; implement a token bucket in Redis to avoid circuit-break bans
- Official portal: https://apiportal.koreainvestment.com/
### Alpha Vantage (US stocks)
- Free tier: 25 requests/day — insufficient for any real workload
- Minimum usable plan: $25/month (30 req/min, no daily cap)
- Consider Finnhub (60 req/min free) as development substitute
- Wrap all external market-data calls in a Circuit Breaker (Spring's `@CircuitBreaker` via Resilience4j) to degrade gracefully when rate-limited
### Yahoo Finance (Backup)
- No official public API; all integrations rely on undocumented endpoints that change without notice
- Use only as last-resort fallback for US prices
- Do not build primary features on Yahoo Finance data availability
### Bank of Korea API (환율 — 한국은행)
- Official REST API for KRW/USD exchange rates
- Cache exchange rates in Redis with 1-hour TTL; do not call per-request
- Free, well-documented, reliable
## Sources
- [Spring Boot 3.5.13 release announcement](https://spring.io/blog/2026/03/26/spring-boot-3-5-13-available-now/) — version verified
- [Spring Boot supported versions / EOL dates](https://github.com/spring-projects/spring-boot/wiki/Supported-Versions) — Boot 3.4 EOL confirmed December 2025
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) — Jackson 3.x, JUnit 6 migration scope
- [Kotlin 2.2.0 release blog](https://blog.jetbrains.com/kotlin/2025/06/kotlin-2-2-0-released/) — version verified
- [Kotlin 2.3.0 release blog](https://blog.jetbrains.com/kotlin/2025/12/kotlin-2-3-0-released/) — 2.3 tooling release noted; 2.2.x recommended for Spring Boot 3.5 stable compatibility
- [React 19.2 release blog](https://react.dev/blog/2025/10/01/react-19-2) — React 19.2 stable confirmed
- [TailwindCSS v4.0 announcement](https://tailwindcss.com/blog/tailwindcss-v4) — v4 stable January 2025 confirmed
- [Vite 8 announcement](https://vite.dev/blog/announcing-vite8) — Vite 8 stable March 2026 confirmed
- [jjwt GitHub repository](https://github.com/jwtk/jjwt) — 0.13.0 latest stable, August 2025
- [Kotlin JDSL GitHub](https://github.com/line/kotlin-jdsl) — v3.x Jakarta-compatible confirmed
- [TradingView Lightweight Charts GitHub](https://github.com/tradingview/lightweight-charts) — canvas-based financial chart library
- [Redis: Jedis vs Lettuce comparison](https://redis.io/blog/jedis-vs-lettuce-an-exploration/) — Lettuce recommended for async/reactive
- [KIS Developers portal](https://apiportal.koreainvestment.com/) — REST + WebSocket, TLS policy
- [Alpha Vantage pricing](https://www.alphavantage.co/premium/) — 25 req/day free tier confirmed
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->

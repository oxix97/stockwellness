# GEMINI.md

This file provides guidance to GEMINI Code (GEMINI.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Full build (runs all tests)
./gradlew clean build

# Run API server (port 8080)
./gradlew :stockwellness-api:bootRun

# Run Batch server (port 8081)
./gradlew :stockwellness-batch:bootRun

# Run tests for a specific module
./gradlew :stockwellness-api:test
./gradlew :stockwellness-core:test
./gradlew :stockwellness-batch:test

# Run a single test class
./gradlew :stockwellness-api:test --tests "org.stockwellness.adapter.in.web.auth.AuthControllerTest"

# Regenerate OpenAPI spec from REST Docs tests
./gradlew updateOpenApiSpec
```

## Local Development Infrastructure

Start local dependencies via Docker Compose before running the application:
```bash
docker compose up -d
```
This starts PostgreSQL (5432), Redis (6379), Zookeeper (2181), and Kafka (9092).

Required environment variables (set in `.env`): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `KAFKA_BOOTSTRAP_SERVERS`, `JWT_SECRET`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `OPENAI_API_KEY`, `KIS_APP_KEY`, `KIS_APP_SECRET`.

## Architecture Overview

This is a **multi-module Spring Boot project** following **Pragmatic Hexagonal Architecture** (Ports & Adapters) combined with DDD.

### Module Dependency Rule
```
stockwellness-api  ──┐
                     ├──► stockwellness-core (domain owner)
stockwellness-batch ─┘
```
- `stockwellness-core` owns all domain entities, business logic (UseCases/Services), persistence adapters, and output ports. It must NOT depend on `api` or `batch`.
- `stockwellness-api` and `stockwellness-batch` are thin adapters — they depend on `core` but never on each other.

### stockwellness-core Internal Layers
```
domain/           ← Pure domain models (no Spring, no JPA annotations at model level)
application/      ← Service layer (orchestration only, no business logic in services)
adapter/
  out/persistence/ ← JPA repositories + QueryDSL custom queries
  out/external/    ← KIS API client, OpenAI adapter
  out/redis/       ← Redis caching adapter
  port/            ← Output port interfaces (implemented by adapters above)
config/            ← JPA, QueryDSL, Kafka, Redis, P6Spy configuration
global/            ← Security, GlobalExceptionHandler, ErrorCode enum
```

### stockwellness-api Internal Layers
```
adapter/in/web/   ← REST Controllers (thin — delegate to core services)
adapter/out/      ← JWT handling, Redis session
config/           ← SecurityConfig, SwaggerConfig, WebConfig
```

### Key Design Patterns
- **PortfolioFacade**: Unified orchestration entry point for all portfolio operations (analysis, backtest, rebalancing, AI advisor)
- **Transactional Outbox**: DB save + Kafka event publish atomicity
- **QueryTypeUtil**: Custom utility for explicit SQL type casting to ensure PostgreSQL/H2 test compatibility
- **AOP Logging**: `LoggingAspect` in `stockwellness-core` provides structured JSON logging across all service boundaries

## API Response Format

All REST endpoints must return the standard wrapper:

**Success:**
```json
{ "data": { ... }, "timestamp": "2026-03-12T17:00:00.000000" }
```

**Error:**
```json
{ "status": 400, "code": "G001", "message": "...", "timestamp": "...", "traceId": "e4e4d65f", "errors": [] }
```

Error codes: `G*` = General, `A*` = Auth, `M*` = Member, `P*` = Portfolio, `S*` = Stock/Sector, `B*` = Batch.
See `org.stockwellness.global.error.ErrorCode` for the full enum.

Throw `GlobalException(ErrorCode.XYZ)` for business exceptions — `GlobalExceptionHandler` normalizes them to `ErrorResponse`.

## Coding Standards

- **Import policy**: Never use FQCN inline (e.g., `java.util.List`). Always import at the top.
- **Immutability**: DTOs, Commands, and Events must use Java `record`.
- **Domain state**: Use `sealed class/interface` to model domain states explicitly.
- **Virtual Threads**: I/O-heavy operations (external API calls, batch steps) use `VirtualThreadPerTaskExecutor`.
- **No password storage**: `Member` entity must never have a `password` field. All auth is OAuth2.
- **EOD data only**: No real-time stock data. Technical indicators (RSI, MACD, etc.) are pre-calculated in batch and stored.
- **Fat Service rule**: Services orchestrate domain models; they do not contain business logic directly.

## Testing

- Integration tests use **H2 in-memory DB** and are in `stockwellness-core/testFixtures/`.
- API tests are Spring REST Docs based — they generate snippets that feed into the OpenAPI spec.
- CI runs tests with real PostgreSQL and Redis service containers (see `.github/workflows/ci.yml`).

## Git Workflow

- Branch from and PR to `develop` (main integration branch).
- Sub-task branches: `task/#<issue>-<description>`
- Feature branches: `feature/#<issue>-<description>` (also: `fix/`, `refactor/`, `chore/`)
- Merge sub-task → feature: `--no-ff`; Merge feature → develop: `--squash`

## Reference Docs

The `conductor/` directory contains design specs and decision records for major features. Key files:
- `conductor/API-standard.md` — API response/error format spec
- `conductor/tech-stack.md` — Full technology stack rationale
- `conductor/product.md` / `conductor/product-guidelines.md` — Product requirements

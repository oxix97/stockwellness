# Synthetic EOD Read Fixture Specification (Task #337)

## Purpose
Minimal, reproducible synthetic End-of-Day (EOD) PostgreSQL fixture for verifying correctness and load readiness of the four target read endpoints:

1. `GET /api/v1/market/indexes`
2. `GET /api/v1/stocks/ranking/supply`
3. `GET /api/v1/stocks/{ticker}/prices/history`
4. `GET /api/v1/portfolios/{portfolioId}/analysis/valuation`

No real production or non-production data is used.

## Fixed Reference Date, Units, and Scale

| Item | Value | Notes |
|---|---|---|
| `asOfDate` (latest `FINAL`) | `2026-11-18` | Within 30-day lookback of `2026-11-19` |
| `previousDate` (`FINAL`) | `2026-11-17` | Used for daily delta / return calculation |
| `earlierDate` (`FINAL`) | `2026-11-16` | Used for multi-day history & cumulative return |
| `provisionalDate` (`PROVISIONAL`) | `2026-11-19` | Trap row; must be excluded by all EOD reads |
| `futureDate` (`FINAL`) | `2099-12-31` | Trap row on prices/indexes; must be excluded by `<= asOf` filters |
| Index price / level unit | Points (`numeric(19,4)`) | Scaled to 2 decimals for `fluctuationRate` (`HALF_UP`) |
| Stock price unit (KRX) | `KRW` (`numeric(19,4)`) | `RAW` vs `PROVIDER_ADJUSTED` preserved separately |
| Supply amount / trading amount | `KRW` (`numeric(25,2)`) | Full KRW (not thousands/millions) |
| Rate / fluctuation unit | Percent `0..100` | e.g., `10.0` means `+10%`, `-5.0` means `-5%` |
| Portfolio valuation amounts | `KRW` (`scale=0`, `HALF_UP`) | `totalPurchaseAmount=1,950,000`, `currentTotalValue=2,050,000` |
| Portfolio valuation rates | Percent `0..100` (`scale=4`, `HALF_UP`) | `totalReturnRate=5.1282`, `dailyReturnRate=2.5000` |

## Synthetic Dataset Summary (Counts & Coverage)

| Table | Rows | Coverage / Edge Cases |
|---|---:|---|
| `trading_calendar` | 4 | `KOSPI` `2026-11-16`..`2026-11-19` |
| `market_index` | 8 | 7 canonical market benchmarks (`0001`, `1001`, `2001`, `3003`, `SPX`, `NDX`, `.DJI`) + 1 sector (`EOD:SECTOR:0013`) |
| `index_series` | 7 | Active canonical `PRICE / NONE` series matching `market(code, currency)` |
| `index_daily` | 23 | 3 `FINAL` days x 7 benchmarks + 1 `PROVISIONAL` (`2026-11-19`) + 1 future `FINAL` (`2099-12-31`) on `0001` |
| `index_weather` | 2 | `0001` `2026-11-18` `FINAL` (`score=80.00` -> `SUNNY / STEADY_ADVANCE`) + `2026-11-19` `PROVISIONAL` trap |
| `stock` | 6 | `990101`..`990106` (`KOSPI`, `KRW`, `ACTIVE`, `sector_index_id=9108`) |
| `stock_price_daily` | 15 | Positive (`+10%`, `+4%`, `+1%`), negative (`-5%`, `-10%`), `PROVISIONAL`-only missing price (`990105`), plus `2026-11-19` & `2099-12-31` traps on `990101` |
| `stock_price_adjustment` | 3 | `990101` split-adjusted history (`4800`, `5000`, `11000`) differing from `RAW` (`9600`, `10000`, `11000`) |
| `stock_technical_indicator` | 3 | `990101` (`RAW`) with valid `input_manifest` matching `revision=1` |
| `stock_investor_trade_daily` | 14 | 12 `FINAL` rows (`BUY` tie on Foreign `990101`/`990102` & Inst `990102`/`990103`; `SELL` tie on Inst `990105`/`990106`; zero Foreign on `990106`) + 2 `PROVISIONAL` traps on `2026-11-19` |
| `member` / `member_identity` | 1 / 1 | `id=9001`, `eod-fixture@synthetic.test` (`KAKAO`), `nickname='eod-fixture-user'` |
| `portfolio` / `portfolio_item` | 1 / 2 | `id=9001`: `990101` (`qty=100 @ 9,500`, `60%`) + `990102` (`qty=50 @ 20,000`, `40%`) |

## Schema & Fixture Checksum Procedure

Always record SHA-256 checksums of the schema SQL files and synthetic fixture files before running verification or benchmark runs:

```bash
shasum -a 256 \
  stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/00-preflight.sql \
  stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/02-schema.sql \
  stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/03-batch-metadata.sql \
  stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/04-reference-data.sql \
  stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/05-verify.sql \
  k6/fixtures/eod-read-seed.sql \
  k6/fixtures/eod-read-expected.json \
  k6/fixtures/eod-read-sql-check.sql
```

## Isolated Provisioning Order

1. Start loopback Postgres, Redis, and Kafka with `.github/test-services/issue-331-compose.yml` under an isolated project name (e.g., `sw-t337-eod`).
2. Apply `00-preflight.sql`, `02-schema.sql`, `03-batch-metadata.sql`, `04-reference-data.sql`, `05-verify.sql` in order with `psql -X -v ON_ERROR_STOP=1`.
3. Apply `k6/fixtures/eod-read-seed.sql` with `psql -X -v ON_ERROR_STOP=1`.
4. Run `k6/fixtures/eod-read-sql-check.sql` to verify adapter SQL parity and capture `EXPLAIN (ANALYZE, BUFFERS)`.
5. Generate an ephemeral 256-bit `JWT_SECRET` and fixed-seed (`memberId=9001`, `USER`) `ACCESS_TOKEN` offline via `EodFixtureTokenIssuer --fixture.secret-out=<path> --fixture.token-out=<path>` (creates `0600` files atomically via `CREATE_NEW`; no Spring/DB/Redis/Kafka/network/`.env` access, no stdout secret/token output, no refresh token or arbitrary identity/role flags).
6. Start the isolated `stockwellness-api` process injecting only the generated `JWT_SECRET` from `--fixture.secret-out`, and pass only `--env-file <token-out>` to `k6` (`analysis-valuation.js`).
   - **API Auth/Authz Verification Scope**: Covers real `JwtAuthenticationFilter` signature verification against the ephemeral `JWT_SECRET`, `CustomUserDetailsService` lookup of seeded PostgreSQL `member` (`id=9001`, `role=USER`), and `PortfolioAnalysisController` ownership authorization for `portfolioId=9001`. OAuth2 login, `/api/v1/auth/exchange`, `/api/v1/auth/reissue`, and Redis refresh-token lifecycle remain out of scope for this offline fixture utility.

## Contract Verification Modes & Quick vs Load Scope

- `EOD_CONTRACT_MODE` defaults to `baseline` (`assertApiSuccess` + baseline `readOptions`/`quickOptions`), keeping `./run-all.sh` and live/staging invocations unchanged.
- `EOD_CONTRACT_MODE=structural` checks real DTO invariants without fixture-only `excludedDates` or non-empty assumptions (allows empty supply lists, `PARTIAL`/`UNAVAILABLE`/zero-KRW valuations, empty `benchmarks` when `includeBenchmark=true`, and nullable `fluctuationRate`/`weather`).
- `EOD_CONTRACT_MODE=fixture` is explicit opt-in strict validation against `eod-read-expected.json` (business-meaningful fields only — omitting stubbed zero `fluctuationAmount` and portfolio net-buying placeholders — plus exact `marketIndexes.exactCount`, full multi-day index `history`, calendar dates, and DTO numeric scales).
- Any unrecognized `EOD_CONTRACT_MODE` fails fast during k6 init before VUs start.
- **Quick vs Load (`short`/`standard`/`stress`)**: `quick` parses and validates every response in full. Load modes run full validation on each VU's first response and on every response whose status `!= 200` or whose raw body differs from the VU's verified body, preserving "1 corrupted response = accuracy failure" while eliminating redundant JSON parsing/allocation on identical cached/EOD responses.

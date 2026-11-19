# k6 실행 명령어 모음

## 목적
`stockwellness/k6` 안의 시나리오를 빠르게 실행할 수 있도록 명령어만 모아둔 문서다. 현재 [compose.k6.yaml](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/compose.k6.yaml:1)에 `env_file: .env`가 연결되어 있으므로, 기본 실행은 `.env` 값을 자동 사용한다.

## 실행 위치
아래 명령어는 모두 같은 위치에서 실행한다.

```bash
cd /Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6
```

## 전체 실행 스크립트
수동으로 하나씩 실행하지 않고 순서대로 돌리려면 [run-all.sh](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/run-all.sh:1)를 사용한다.

기본 실행:

```bash
./run-all.sh
```

모드별 실행:

```bash
./run-all.sh quick
./run-all.sh short
./run-all.sh standard
./run-all.sh stress
./run-all.sh write
./run-all.sh all
```

의미:
- `quick`: 시나리오당 10초, 인증/기능 검증
- `short`: 시나리오당 2분, 빠른 성능 회귀 확인
- `standard`: 시나리오당 5분, 성과 기록용 부하 측정
- `stress`: 시나리오당 10분, 고부하 안정성 확인
- `write`: `auth-login`, `portfolio-create`, `watchlist-group-create`
- `all`: `standard` + `write`

동작:
- threshold 실패가 나도 다음 시나리오를 계속 실행한다.
- 각 시나리오 로그는 `results/logs/<실행시각>/`에 저장된다.
- 전체 요약은 `results/run-all-summary-<실행시각>.md`에 저장된다.
- 하나라도 실패하면 스크립트 마지막 종료 코드는 `1`이다.

## 기본 전제
`.env`에 아래 값이 준비되어 있어야 한다.

```bash
BASE_URL=https://stockwellness.duckdns.org
ACCESS_TOKEN=YOUR_ACCESS_TOKEN
PORTFOLIO_ID=18
GROUP_ID=7
TICKER=005930
SECTOR_CODE=G25
SEARCH_KEYWORD=삼성
K6_SUMMARY_TREND_STATS=avg,min,med,max,p(90),p(95),p(99)
```

필요할 때만 추가로 사용한다.

```bash
BACKTEST_AMOUNT=10000000
BACKTEST_PERIOD=1Y
BENCHMARK_TICKER=SPY
LIMIT=10
MARKET_TYPE=
SUPPLY_DIRECTION=BUY
PERIOD=1Y
FREQUENCY=DAILY
INCLUDE_BENCHMARK=false
PAGE=0
SIZE=20
LOGIN_EMAIL=k6.live@example.com
LOGIN_NICKNAME=k6-live
LOGIN_TYPE=NONE
PORTFOLIO_NAME=k6-live-portfolio
PORTFOLIO_DESCRIPTION=k6 live performance test portfolio
GROUP_NAME=k6-live-group
REFRESH_TOKEN=YOUR_REFRESH_TOKEN
```

## Smoke
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/smoke-live.json \
  /scripts/scenarios/smoke.js
```

## 공개 읽기 API
### market-indexes
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/market-indexes-standard.json \
  /scripts/scenarios/market-indexes.js
```

### stock-search
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/stock-search-standard.json \
  /scripts/scenarios/stock-search.js
```

### stock-popular-search
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/stock-popular-search-standard.json \
  /scripts/scenarios/stock-popular-search.js
```

### stock-new-listings
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/stock-new-listings-standard.json \
  /scripts/scenarios/stock-new-listings.js
```

### stock-detail
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/stock-detail-standard.json \
  /scripts/scenarios/stock-detail.js
```

### stock-supply-ranking
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/stock-supply-ranking-standard.json \
  /scripts/scenarios/stock-supply-ranking.js
```

### stock-price-history
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/stock-price-history-standard.json \
  /scripts/scenarios/stock-price-history.js
```

### stock-returns
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/stock-returns-standard.json \
  /scripts/scenarios/stock-returns.js
```

### sector-fluctuation-ranking
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/sector-fluctuation-ranking-standard.json \
  /scripts/scenarios/sector-fluctuation-ranking.js
```

### sector-comparison
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/sector-comparison-standard.json \
  /scripts/scenarios/sector-comparison.js
```

### sector-detail
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/sector-detail-standard.json \
  /scripts/scenarios/sector-detail.js
```

## 인증 읽기 API
### member-me
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/member-me-standard.json \
  /scripts/scenarios/member-me.js
```

### member-notifications
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/member-notifications-standard.json \
  /scripts/scenarios/member-notifications.js
```

### portfolio-list
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/portfolio-list-standard.json \
  /scripts/scenarios/portfolio-list.js
```

### portfolio-detail
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/portfolio-detail-standard.json \
  /scripts/scenarios/portfolio-detail.js
```

### portfolio-health
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/portfolio-health-standard.json \
  /scripts/scenarios/portfolio-health.js
```

### portfolio-advice-latest
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/portfolio-advice-latest-standard.json \
  /scripts/scenarios/portfolio-advice-latest.js
```

### analysis-valuation
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/analysis-valuation-standard.json \
  /scripts/scenarios/analysis-valuation.js
```

### analysis-diversification
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/analysis-diversification-standard.json \
  /scripts/scenarios/analysis-diversification.js
```

### analysis-rebalancing
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/analysis-rebalancing-standard.json \
  /scripts/scenarios/analysis-rebalancing.js
```

### analysis-summary
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/analysis-summary-standard.json \
  /scripts/scenarios/analysis-summary.js
```

### analysis-correlation
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/analysis-correlation-standard.json \
  /scripts/scenarios/analysis-correlation.js
```

### analysis-inception-performance
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/analysis-inception-performance-standard.json \
  /scripts/scenarios/analysis-inception-performance.js
```

### analysis-inception-chart
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/analysis-inception-chart-standard.json \
  /scripts/scenarios/analysis-inception-chart.js
```

### watchlist-groups
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/watchlist-groups-standard.json \
  /scripts/scenarios/watchlist-groups.js
```

### watchlist-items
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/watchlist-items-standard.json \
  /scripts/scenarios/watchlist-items.js
```

### stock-search-history
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/stock-search-history-standard.json \
  /scripts/scenarios/stock-search-history.js
```

## 쓰기 보조 시나리오
### auth-login
`ACCESS_TOKEN`이 없을 때 1회 로그인 응답을 확인하는 용도다.

```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  /scripts/scenarios/auth-login.js
```

### portfolio-create
`PORTFOLIO_ID`가 없을 때 테스트용 포트폴리오를 1회 생성한다.

```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  /scripts/scenarios/portfolio-create.js
```

### watchlist-group-create
`GROUP_ID`가 없을 때 테스트용 관심종목 그룹을 1회 생성한다.

```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  /scripts/scenarios/watchlist-group-create.js
```

## 부하 시나리오
### backtest-heavy
```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/backtest-heavy-standard.json \
  /scripts/scenarios/backtest-heavy.js
```

### auth-reissue-burst
`REFRESH_TOKEN`이 필요하다. refresh token rotation 영향이 있으므로 반복 재사용 결과는 왜곡될 수 있다.

```bash
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  --summary-export /scripts/results/auth-reissue-burst-standard.json \
  /scripts/scenarios/auth-reissue-burst.js
```

## 결과 요약 확인
단일 결과 파일 확인:

```bash
jq '{
  p95: .metrics.http_req_duration["p(95)"],
  p99: .metrics.http_req_duration["p(99)"],
  failed_rate: .metrics.http_req_failed.value,
  request_count: .metrics.http_reqs.count,
  checks: .metrics.checks.value
}' /Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/results/smoke-live.json
```

여러 결과 파일 한 번에 확인:

```bash
for f in /Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/results/*-standard.json; do
  echo -n "$(basename "$f") "
  jq -r '[
    .metrics.http_req_duration["p(95)"],
    .metrics.http_req_duration["p(99)"],
    .metrics.http_req_failed.value,
    .metrics.http_reqs.count
  ] | @tsv' "$f"
done
```

## 권장 실행 순서
1. `smoke.js`
2. 공개 읽기 API 전체
3. 인증 읽기 API 전체
4. 필요하면 `portfolio-create.js`, `watchlist-group-create.js`
5. `backtest-heavy.js`
6. 필요하면 `auth-reissue-burst.js`

## 격리 합성 EOD 검증 (Task #337)
대상 시나리오(`market-indexes.js`, `stock-supply-ranking.js`, `stock-price-history.js`, `analysis-valuation.js`)는 `EOD_CONTRACT_MODE`를 지정하지 않으면 기본 `baseline` 모드(기존 `assertApiSuccess` + `readOptions`/`quickOptions`)로 동작하여 기존 `./run-all.sh` 및 문서화된 호출과 호환된다. 알 수 없는 `EOD_CONTRACT_MODE`(예: `fixtur`)는 VU 실행 전에 즉시 오류로 중단된다.

- `EOD_CONTRACT_MODE=baseline` (기본값): HTTP 200 및 `data` 존재 여부(`assertApiSuccess`), 기존 baseline threshold 유지
- `EOD_CONTRACT_MODE=structural` (명시 opt-in): 일반/실서버 호환 구조·수학 불변식 검증(빈 수급 목록, `PARTIAL`/`UNAVAILABLE`/0원 평가, `includeBenchmark=true` 빈 벤치마크, `null` weather/등락률 허용, fixture `excludedDates` 미적용)
- `EOD_CONTRACT_MODE=fixture` (명시 opt-in): `k6/fixtures/eod-read-expected.json` 대조 엄격 검증(`checks: rate==1`, `eod_contract_failures: count==0`, `http_req_failed: rate==0`, 실존 달력일·DTO 스케일·`exactCount`·전체 지수 `history`·사업 의미 필드 비교).
  - `TICKER=990101` (`stock-price-history.js`)
  - `PORTFOLIO_ID=9001` (`analysis-valuation.js`)
  - `SUPPLY_DIRECTION=BUY` 및 `SELL` (`stock-supply-ranking.js`)
- **`quick` vs 부하(`short`/`standard`/`stress`) 체크 범위**: `quick`에서는 매 요청마다 전체 JSON 파싱과 계약 검증을 수행하고, 부하 모드에서는 각 VU의 첫 응답과 응답 본문(`res.body`)이 직전 검증 응답과 달라진 모든 응답에서 전체 계약 검증을 수행한다(동일 `200` 본문 재응답 시 반복 파싱·실패 배열 할당 생략, 단 1건의 손상 응답도 즉시 `eod_contract_failures` 및 `checks` 실패 처리).

### 1. 격리 DB + 합성 fixture 체크섬 확인
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

### 2. SQL 결과 대조 및 EXPLAIN (ANALYZE, BUFFERS)
```bash
psql -X -v ON_ERROR_STOP=1 "$ISOLATED_DB_URL" -f k6/fixtures/eod-read-sql-check.sql
```

### 3. 오프라인 테스트용 JWT 키·토큰 발급 및 격리 API 기동 (1회성 키)
`EodFixtureTokenIssuer`는 Spring·DB·Redis·Kafka·외부 네트워크·`.env`를 전혀 사용하지 않고 매 실행 새 무작위 256-bit `JWT_SECRET`과 고정 seed 사용자(`memberId=9001`, `role=USER`, `eod-fixture@synthetic.test`)의 access token만 기존 `JwtProvider`로 생성해 각각 `0600`/`CREATE_NEW` 파일로 저장한다(stdout 미출력, refresh token·외부 키 재사용·임의 id/role 입력 미지원).

```bash
./gradlew :stockwellness-api:bootJar :stockwellness-api:testClasses

ISO_TMP="$(mktemp -d)"
unzip -j -q stockwellness-api/build/libs/stockwellness-api-*.jar 'BOOT-INF/lib/*.jar' -d "$ISO_TMP/libs"
java -cp "stockwellness-api/build/classes/java/test:stockwellness-api/build/classes/java/main:$ISO_TMP/libs/*" \
  org.stockwellness.support.EodFixtureTokenIssuer \
  --fixture.secret-out="$ISO_TMP/eod-secret.env" \
  --fixture.token-out="$ISO_TMP/eod-token.env"
```

- 격리 API 프로세스 기동 시 `$ISO_TMP/eod-secret.env`의 `JWT_SECRET`만 주입한다(운영/기존 `.env` 미사용).
- **API 인증·인가 검증 범위**: 격리 API의 `JwtAuthenticationFilter`가 1회성 `JWT_SECRET`으로 토큰을 검증하고, `CustomUserDetailsService`가 격리 PostgreSQL의 seed `member`(`id=9001`, `role=USER`)를 로드한 뒤 `PortfolioAnalysisController`의 포트폴리오 소유권(`portfolioId=9001`) 인가까지 실제 경로로 검증한다. OAuth2 콜백·`/api/v1/auth/exchange`·`/api/v1/auth/reissue` 및 Redis refresh token 수명주기는 이 도구의 검증 범위가 아니다.

### 4. 격리 quick 사전 검증 (1 VU, 10초, opt-in fixture 모드)
```bash
docker run --rm -i \
  --add-host=host.docker.internal:host-gateway \
  -v "$(pwd)/k6:/scripts:ro" \
  -e BASE_URL="http://host.docker.internal:18080" \
  -e K6_MODE=quick \
  -e EOD_CONTRACT_MODE=fixture \
  grafana/k6:latest run /scripts/scenarios/market-indexes.js

docker run --rm -i \
  --add-host=host.docker.internal:host-gateway \
  -v "$(pwd)/k6:/scripts:ro" \
  -e BASE_URL="http://host.docker.internal:18080" \
  -e K6_MODE=quick \
  -e EOD_CONTRACT_MODE=fixture \
  -e SUPPLY_DIRECTION=BUY \
  -e LIMIT=10 \
  grafana/k6:latest run /scripts/scenarios/stock-supply-ranking.js

docker run --rm -i \
  --add-host=host.docker.internal:host-gateway \
  -v "$(pwd)/k6:/scripts:ro" \
  -e BASE_URL="http://host.docker.internal:18080" \
  -e K6_MODE=quick \
  -e EOD_CONTRACT_MODE=fixture \
  -e TICKER=990101 \
  -e PERIOD=1Y \
  -e FREQUENCY=DAILY \
  -e INCLUDE_BENCHMARK=true \
  grafana/k6:latest run /scripts/scenarios/stock-price-history.js

docker run --rm -i \
  --add-host=host.docker.internal:host-gateway \
  -v "$(pwd)/k6:/scripts:ro" \
  --env-file "$ISO_TMP/eod-token.env" \
  -e BASE_URL="http://host.docker.internal:18080" \
  -e K6_MODE=quick \
  -e EOD_CONTRACT_MODE=fixture \
  -e PORTFOLIO_ID=9001 \
  grafana/k6:latest run /scripts/scenarios/analysis-valuation.js
```

### 5. 오프라인 JS 계약 회귀 및 토큰 발급기 단위 테스트
```bash
node --test k6/tests/eod-contract.test.mjs
./gradlew :stockwellness-api:test --tests 'org.stockwellness.support.EodFixtureTokenIssuerTest'
```

## 합성 백테스트 계약 검증 (Task #339)

`BACKTEST_CONTRACT_MODE=fixture`는 전용 격리 DB에서만 사용한다. 먼저 위 #337 스키마와 seed를 적용하고, 별도 DB에서 `psql -X -v ON_ERROR_STOP=1 "$ISOLATED_DB_URL" -f k6/fixtures/backtest-seed.sql`을 적용한다. 이 추가 seed는 백테스트 날짜 합집합을 고정하기 위해 해당 DB의 KOSPI·KOSDAQ·SP500 지수 일별 행을 삭제하므로 #337 읽기 검증용 DB와 공유하지 않는다. 운영 DB에서는 실행하지 않는다.

테스트 사용자 9001의 포트폴리오 9003은 합성 종목 990107 하나를 100% 보유한다. 요청 금액은 1,000원, 기간은 ALL, 배당 재투자와 리밸런싱은 꺼져 있다. 주가 100→110→110, KOSPI 100→105→110의 날짜는 2025-01-02, 2025-01-15, 2026-01-02다. #338 `BacktestEngineTest.dca_one_year_exact_xirr_and_same_month_single_deposit`의 독립 계산과 동일한 입력으로, 거치식은 투자 1,000원·평가 1,100원·손익 +100원·CAGR 10%, 적립식은 투자 2,000원·평가 2,100원·손익 +100원·XIRR 10%다. 두 전략 TWR은 10%이며 API `totalReturnRate`도 TWR이다. 날짜·기준 벤치마크·일별 평가·입금·일별 수익률·벤치마크 수익률과 코드별 수익률 및 전략별 지표를 매 응답 대조한다. 두 전략의 일별 수익률은 각각 0%, 10%, 0%이고 KOSPI 누적 수익률은 0%, 5%, 10%다.

격리 API는 외부 호출이 차단된 상태로 별도 기동하고 위 #337 절의 임시 JWT 발급 방법을 사용한다. 토큰은 파일로만 전달하고 로그에 출력하지 않는다. 컨테이너의 `/scripts`와 토큰 파일 마운트는 기존 k6 실행 방식에 맞춘다.

아래는 **warm 조건**의 예시다. 양 전략 quick이 모두 통과해야 heavy를 실행하며, 어느 heavy 회차든 실패하면 남은 모든 회차를 중단하고 전체 명령도 실패한다. 각 실행의 요약 JSON·로그·종료 코드를 별도 보관하고 실패 원인은 로그와 지표에서 정확성 실패 또는 성능 threshold 실패로 구분한다. 결과 디렉터리는 새로 생성되므로 기존 원본을 덮어쓰지 않는다.

```bash
run_backtest_measurements() {
  local results strategy run rc phase
  results="$(mktemp -d "${TMPDIR:-/tmp}/sw339-warm.XXXXXX")" || return 1
  printf 'planned_cache_condition=warm\n' > "$results/cache-condition.txt"
  printf '결과 디렉터리: %s\n' "$results"

  for phase in quick heavy; do
    for strategy in LUMP_SUM DCA; do
      for run in $(if [ "$phase" = quick ]; then printf '1'; else printf '1 2 3'; fi); do
        if docker run --rm --network "$ISOLATED_K6_NETWORK" \
          --add-host=host.docker.internal:host-gateway \
          --user "$(id -u):$(id -g)" \
          -v "$PWD/k6:/scripts:ro" -v "$results:/results" --env-file "$TOKEN_FILE" \
          grafana/k6:latest run --summary-export "/results/${phase}-${strategy}-${run}.json" \
          -e BASE_URL="$ISOLATED_API_URL" -e PORTFOLIO_ID=9003 \
          -e BACKTEST_CONTRACT_MODE=fixture -e BACKTEST_STRATEGY="$strategy" \
          -e K6_MODE="$(if [ "$phase" = quick ]; then printf quick; else printf standard; fi)" \
          /scripts/scenarios/backtest-heavy.js > "$results/${phase}-${strategy}-${run}.log" 2>&1; then
          rc=0
        else
          rc=$?
        fi
        printf '%s\n' "$rc" > "$results/${phase}-${strategy}-${run}.exit"
        if [ "$rc" -ne 0 ]; then
          printf '%s %s %s회 실패: 남은 실행 중단; 원본은 %s에 보관\n' "$phase" "$strategy" "$run" "$results" >&2
          return "$rc"
        fi
      done
    done
  done
}
run_backtest_measurements
```

실행이 시작 전에 실패하면 JSON이 생성되지 않을 수 있으므로 해당 사실과 로그를 보존한다. 각 `.exit`와 JSON·로그를 함께 확인하고 오류·빈 응답·금융값 불일치가 있으면 해당 실행을 정확성 실패로 기록한다. quick 자체가 캐시를 예열할 수 있으므로 위 연속 실행을 cold로 간주하지 않는다. cold 측정은 격리 환경에서 회차별 초기화 방법과 직전 상태를 확인하고, warm 측정은 예열 방법과 횟수를 기록해 각각 별도 결과 디렉터리에 보관한다. 회차별 캐시 상태를 입증할 수 없으면 미확인으로 기록한다. p95/p99·요청 수·RPS·HTTP 오류·checks와 종료 코드, 동시간대 CPU·메모리·DB·연결 풀·Redis 지표를 기록하고 수집하지 못한 항목은 미수집으로 남긴다.

fixture 모드에서 `K6_MODE=quick`은 1 VU·10초, 그 외는 3 VU·3분이다. baseline 모드에서는 기존 quick 또는 `readOptions`의 short·standard·stress 부하 프로필을 유지한다. fixture 모드는 HTTP 실패, 빈 응답, 잘못된 값이 한 건이라도 있으면 `backtest_contract_failures`와 엄격한 threshold가 실패한다. 기본 `baseline`은 기존 요청 필드와 기본값을 유지하며, `BACKTEST_STRATEGY=DCA`로 적립식을 선택할 수 있다. 실제 API와 DB·Redis 격리, 결과 원본과 SHA 검증은 별도 단계이며, 위 예시는 성공을 보증하지 않는다. #338의 4원→6원 리밸런싱 반올림 관측은 금융 정확성 실패로 별도 기록해야 한다.

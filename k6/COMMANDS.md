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

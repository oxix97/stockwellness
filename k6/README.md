# k6 Load Test Guide

## 목적
Stockwellness 백엔드 핵심 API의 성능을 Docker 기반 `grafana/k6`로 측정하고, 개선 전후 수치를 비교해 성과 자료로 활용한다.

최근 측정 결과 보고서는 [PERFORMANCE-REPORT-20260423.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/PERFORMANCE-REPORT-20260423.md)를 참고한다.

## 대상 API
- `GET /api/v1/auth/test`
- `GET /actuator/health`
- `GET /api/v1/portfolios/{portfolioId}/analysis/summary`
- `GET /api/v1/portfolios/{portfolioId}/health`
- `POST /api/v1/portfolios/{portfolioId}/analysis/backtest`

## 실행 위치
반드시 `/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6`에서 실행한다.

## 사전 준비
1. Docker 실행
2. 백엔드 인프라 기동
3. API 서버 실행
4. 테스트용 access token 준비
5. 테스트용 portfolioId 준비

## 필수 환경변수
- `BASE_URL`
- `ACCESS_TOKEN`
- `PORTFOLIO_ID`

선택:
- `BACKTEST_AMOUNT`
- `BACKTEST_PERIOD`
- `BENCHMARK_TICKER`
- `GROUP_ID`
- `TICKER`
- `SECTOR_CODE`
- `SEARCH_KEYWORD`
- `K6_SUMMARY_TREND_STATS`

기본 실서버 예시:

```bash
BASE_URL=https://stockwellness.duckdns.org
```

로컬 서버를 붙일 때만 아래처럼 사용한다.

```bash
BASE_URL=http://host.docker.internal:8080
```

실서버용 전체 예시는 [k6/.env.live.example](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/.env.live.example) 참고.

## 시나리오 명명 기준
실행 기준 이름은 아래로 통일한다.

- `portfolio-health.js`
- `analysis-summary.js`

기존 파일:

- `health-read.js`
- `summary-read.js`

는 호환용으로 유지한다. 새 실행과 문서에서는 `portfolio-health.js`, `analysis-summary.js`를 우선 사용한다.

## 읽기 API 실행 순서
실행은 공개 API와 인증 API를 분리해서 아래 순서로 진행한다.

### 공개 읽기 API
1. `market-indexes.js`
2. `stock-search.js`
3. `stock-popular-search.js`
4. `stock-new-listings.js`
5. `stock-detail.js`
6. `stock-supply-ranking.js`
7. `stock-price-history.js`
8. `stock-returns.js`
9. `sector-fluctuation-ranking.js`
10. `sector-comparison.js`
11. `sector-detail.js`

### 인증 읽기 API
1. `member-me.js`
2. `member-notifications.js`
3. `portfolio-list.js`
4. `portfolio-detail.js`
5. `portfolio-health.js`
6. `portfolio-advice-latest.js`
7. `analysis-valuation.js`
8. `analysis-diversification.js`
9. `analysis-rebalancing.js`
10. `analysis-summary.js`
11. `analysis-correlation.js`
12. `analysis-inception-performance.js`
13. `analysis-inception-chart.js`
14. `watchlist-groups.js`
15. `watchlist-items.js`

## 실행 예시

### Smoke
```bash
cd /Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  -e BASE_URL=https://stockwellness.duckdns.org \
  --summary-export /scripts/results/smoke.json \
  /scripts/scenarios/smoke.js
```

### Summary Read
```bash
cd /Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  -e BASE_URL=https://stockwellness.duckdns.org \
  -e ACCESS_TOKEN=YOUR_ACCESS_TOKEN \
  -e PORTFOLIO_ID=1 \
  --summary-export /scripts/results/summary-read-before-1.json \
  /scripts/scenarios/summary-read.js
```

### Health Read
```bash
cd /Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  -e BASE_URL=https://stockwellness.duckdns.org \
  -e ACCESS_TOKEN=YOUR_ACCESS_TOKEN \
  -e PORTFOLIO_ID=1 \
  --summary-export /scripts/results/health-read-before-1.json \
  /scripts/scenarios/health-read.js
```

### Backtest Heavy
```bash
cd /Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6
docker compose -f compose.k6.yaml run --rm \
  k6 run \
  -e BASE_URL=https://stockwellness.duckdns.org \
  -e ACCESS_TOKEN=YOUR_ACCESS_TOKEN \
  -e PORTFOLIO_ID=1 \
  -e BACKTEST_AMOUNT=10000000 \
  -e BACKTEST_PERIOD=1Y \
  -e BENCHMARK_TICKER=SPY \
  --summary-export /scripts/results/backtest-before-1.json \
  /scripts/scenarios/backtest-heavy.js
```

## 결과 파일 규칙
- `summary-read-before-1.json`
- `summary-read-before-2.json`
- `summary-read-before-3.json`
- `summary-read-after-1.json`
- `summary-read-after-2.json`
- `summary-read-after-3.json`

다른 API도 같은 규칙을 따른다.

## 실행 순서
1. `smoke.js` 1회
2. `summary-read.js` 개선 전 3회
3. `health-read.js` 개선 전 3회
4. `backtest-heavy.js` 개선 전 3회
5. 성능 개선 작업
6. 동일 시나리오 개선 후 3회
7. 결과 비교표 작성

## 결과 추출
단일 파일:
```bash
jq '{
  p95: .metrics.http_req_duration.values["p(95)"],
  p99: .metrics.http_req_duration.values["p(99)"],
  failed_rate: .metrics.http_req_failed.values.rate,
  request_count: .metrics.http_reqs.values.count
}' results/summary-read-before-1.json
```

여러 파일:
```bash
for f in results/summary-read-*.json; do
  echo -n "$(basename "$f") "
  jq -r '[
    .metrics.http_req_duration.values["p(95)"],
    .metrics.http_req_duration.values["p(99)"],
    .metrics.http_req_failed.values.rate,
    .metrics.http_reqs.values.count
  ] | @tsv' "$f"
done
```

## 주의사항
- `auth-reissue-burst.js`는 refresh token rotation 때문에 동일 토큰 재사용 시 결과가 왜곡될 수 있다.
- 이력서용 성과 지표는 `avg`보다 `p95`를 우선 사용한다.
- 최소 3회 반복 후 평균값으로 비교한다.

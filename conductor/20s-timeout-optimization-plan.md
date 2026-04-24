# 20초 타임아웃 및 4초 병목 최적화 계획

**Goal:** `k6` 성능 테스트에서 확인된 20초 타임아웃 이슈(HikariCP 커넥션 풀 고갈) 및 `market-indexes`의 4초(p95) 조회 병목 현상을 캐싱 및 설정 최적화를 통해 해결합니다.

**Root Causes:**
1. **P6Spy 및 DEBUG 로깅 과부하**: `application-core.yaml`에 `p6spy`와 `org.stockwellness: DEBUG`가 활성화되어 있어 부하 테스트 중 대량의 동기식 I/O 병목이 발생하여 커넥션 타임아웃(20초)을 유발했습니다.
2. **Missing Caches**:
   - `StockPriceCacheAdapter.loadPricesByYear`에 캐시가 누락되어 1년치 차트 데이터를 조회할 때마다 N번의 조인 쿼리가 실행됩니다.
   - `StockChartService.getTopStocksBySupply` (수급 랭킹) 호출 시 무거운 Join 쿼리가 캐시 없이 매번 실행됩니다.
   - `StockPriceAdapter.summarizeBreadthByDate` (시장 상승/하락 폭 계산)가 3.5백만 건의 데이터를 매번 집계합니다.

---

### Task 1: 애플리케이션 기본 로깅 최적화 (I/O 병목 제거)

**Files to modify:** `stockwellness-core/src/main/resources/application-core.yaml`

- `p6spy.enable-logging`을 `false`로 기본 변경.
- `logging.level.org.stockwellness`를 `INFO`로 상향.
- `logging.level.org.springframework.web.client.RestClient` 및 `org.springframework.ai`를 `WARN`으로 상향.

### Task 2: 캐시 타입(CacheType) 추가

**Files to modify:** `stockwellness-core/src/main/java/org/stockwellness/domain/common/cache/CacheType.java`

- 다음 항목을 Enum에 추가:
  - `STOCK_PRICE_YEAR("stockPriceYear:v1", Duration.ofHours(24), false)`
  - `MARKET_BREADTH("marketBreadth:v1", Duration.ofHours(24), false)`
  - `STOCK_SUPPLY_RANKING("stockSupplyRanking:v1", Duration.ofMinutes(10), false)`

### Task 3: 무거운 DB 조회 및 집계 메서드에 캐시 적용

**Files to modify:** 
- `stockwellness-core/src/main/java/org/stockwellness/adapter/out/persistence/stock/StockPriceCacheAdapter.java`
- `stockwellness-core/src/main/java/org/stockwellness/adapter/out/persistence/stock/StockPriceAdapter.java`
- `stockwellness-core/src/main/java/org/stockwellness/application/service/stock/StockChartService.java`

- `StockPriceCacheAdapter`의 `loadPricesByYear` 메서드에 `@Cacheable(cacheNames = "stockPriceYear:v1", key = "#ticker + '_' + #year")` 적용.
- `StockPriceAdapter`의 `summarizeBreadthByDate` 메서드에 `@Cacheable(cacheNames = "marketBreadth:v1", key = "#baseDate.toString()")` 적용. (Spring의 빈 프록시를 타기 위해 자기참조 또는 별도 어댑터 주입 필요, 혹은 `MarketIndexService` 단에서 캐싱 고려)
  - *대안*: `MarketIndexService` 내에서 호출하는 부분 자체는 이미 `marketDashboard`로 캐시되어 있으므로, `summarizeBreadthByDate` 개별 캐시는 필수적이지 않을 수 있습니다. 그러나 향후 재사용을 위해 `StockPriceCacheAdapter`로 위임하여 캐싱하는 것이 안전합니다.
- `StockChartService`의 `getTopStocksBySupply` 메서드에 `@Cacheable(cacheNames = "stockSupplyRanking:v1", key = "#direction + '_' + #limit")` 적용.

### Task 4: 통합 테스트 실행 (k6)

- 최적화 적용 후 `k6/run-all.sh quick` 및 `standard` 모드로 다시 실행하여 타임아웃(20초) 해결 여부 및 `market-indexes` 4초 지연 현상 개선(p95 < 100ms 목표)을 검증합니다.

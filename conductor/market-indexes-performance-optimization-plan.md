# market-indexes API 성능 병목 개선 (Issue #226)

## 1. 개요
현재 `market-indexes` API는 기능적으로 정상이지만, 대규모 부하 발생 시 캐시 갱신 지연(`sync = true`)으로 인해 p95 응답 속도가 3.9초까지 치솟는 심각한 성능 병목을 보이고 있습니다.
근본 원인은 캐시 미스 시 수행되는 9개의 벤치마크 단건 조회와 약 3,600건의 `MarketBreadthItem`을 메모리에 올려 `BigDecimal`로 연산하는 과정입니다.

본 작업에서는 **QueryDSL을 활용하여 앱 서버의 메모리/CPU 부하를 데이터베이스의 효율적인 집계 연산으로 전환**하고, 쿼리 왕복 횟수를 대폭 줄여 근본적인 병목을 해소합니다.

## 2. 작업 대상 파일
*   `BenchmarkRepository.java` / `BenchmarkRepositoryImpl.java`: 지수 일괄 조회 기능 추가
*   `LoadBenchmarkPort.java` / `StockPriceAdapter.java`: 어댑터에 일괄 조회 포트 추가
*   `StockPriceRepositoryCustom.java` / `StockPriceRepositoryImpl.java`: 등락 분포 직접 집계 쿼리 추가
*   `MarketIndexService.java`: 단건 순회 로직 제거 및 DB 기반 집계 로직 적용

## 3. 구현 단계

### Step 1. Benchmark 다건 일괄 조회 쿼리 추가
*   `BenchmarkRepository`에 `findBenchmarkPricesIn(List<String> tickers, LocalDate start, LocalDate end)` 메서드를 추가합니다.
*   `BenchmarkRepositoryImpl`에서 `ticker.in(tickers)` 조건으로 조회한 후, `Map<String, List<StockPriceResult>>` 또는 평탄화된 리스트로 반환하여 서비스 단에서 그룹화하도록 구현합니다.

### Step 2. 시장 등락 분포(Breadth) DB 레벨 집계 (QueryDSL)
*   `StockPriceRepositoryCustom`에 `summarizeBreadthByDate(LocalDate baseDate)` 메서드를 추가합니다.
*   `StockPriceRepositoryImpl`에서 QueryDSL의 `Expressions.cases().when(...).then(1).otherwise(0).sum()` 등의 구문을 사용해, 전체/상승/하락/보합/급등락 건수를 한 번의 쿼리로 계산하고 `MarketBreadthSnapshot`(또는 중간 DTO)으로 직접 반환받도록 작성합니다.
    *   *참고:* DB 내부에서 `(close_price - COALESCE(prev_close_price, open_price))` 연산을 수행하여 등락률과 장중 변동성을 계산합니다.

### Step 3. MarketIndexService 리팩토링
*   `BenchmarkType.values()`를 순회하며 9번 발생하던 `loadBenchmarkPrices` 호출을 Step 1의 `findBenchmarkPricesIn`으로 통합하여 1회로 줄입니다.
*   `stockPricePort.findAllBreadthItemsByDate`로 3,600건을 가져오던 로직을 지우고, Step 2의 `summarizeBreadthByDate`를 호출하여 결과만 받아오도록 수정합니다.
*   기존 자바 레벨 집계 유틸리티인 `MarketBreadthCalculator`의 사용을 제거(또는 다른 용도를 위해 유지)합니다.

## 4. 검증 및 테스트
*   로컬에서 `k6/scenarios/market-indexes.js`를 재실행하여 p95가 800ms 이하, 안정 시 수 ms 이하로 극적으로 개선되는지 확인합니다.
*   `MarketIndexServiceTest` 등 기존 단위 테스트에서 목(mock) 설정이 변경됨에 따라 깨진 부분을 수정하고, 결과 값이 기존 연산 로직과 동일하게 유지되는지 검증합니다.

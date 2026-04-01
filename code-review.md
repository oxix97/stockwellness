# 리뷰 보고서: 해외 지수 업데이트 및 배치 제어 레이어 구현

## 요약
해외 지수 시세 수집을 위한 KIS API 호환성 업데이트와 배치를 자동화/수동 실행하기 위한 제어 계층이 구현되었습니다. 전체적인 아키텍처 흐름은 적절하나, **N+1 쿼리로 인한 성능 저하** 및 **API 응답 타임아웃 위험** 등 운영 환경에서 문제가 될 수 있는 고위험 요소들이 발견되었습니다.

---

## File: stockwellness-api/src/main/java/org/stockwellness/adapter/in/web/batch/BatchAdminController.java
### L46: [MEDIUM] 동기 방식의 배치 실행으로 인한 API 타임아웃 위험
Spring Batch의 `JobLauncher.run()`은 기본적으로 동기(Synchronous) 방식으로 동작합니다. 시세 수집과 같이 실행 시간이 긴 배치를 컨트롤러에서 직접 호출할 경우, 배치가 완료될 때까지 HTTP 연결이 유지되어 타임아웃이 발생할 수 있습니다.

**상세 내용:** 2년 치 데이터를 수집하는 등의 케이스에서 API 응답이 수 분 이상 지연될 수 있습니다.

Suggested change:
```java
-            jobLauncher.run(benchmarkPriceSyncJob, params);
-            return ResponseEntity.ok("Benchmark price sync job started successfully");
+            // 비동기 실행을 위해 CompletableFuture 또는 전용 TaskExecutor 사용 권장
+            CompletableFuture.runAsync(() -> {
+                try {
+                    jobLauncher.run(benchmarkPriceSyncJob, params);
+                } catch (Exception e) {
+                    log.error("[Batch Admin] 비동기 배치 실행 실패", e);
+                }
+            });
+            return ResponseEntity.accepted().body("Benchmark price sync job submitted");
```

---

## File: stockwellness-core/src/main/java/org/stockwellness/application/service/portfolio/PortfolioQueryService.java
### L53: [HIGH] getLatestPrices 메서드 내 N+1 쿼리 발생
포트폴리오의 각 종목에 대해 `stockPricePort.findLatestByTicker`를 루프 안에서 호출하고 있습니다. 특히 `getMyPortfolios` 호출 시 "포트폴리오 수 × 종목 수"만큼 쿼리가 발생하여 DB 부하가 급증합니다.

**상세 내용:** 다수의 사용자가 포트폴리오 목록을 조회할 경우 성능이 기하급수적으로 저하됩니다.

Suggested change:
```java
     private Map<String, BigDecimal> getLatestPrices(Portfolio portfolio) {
-        return portfolio.getItems().stream()
-                .map(PortfolioItem::getSymbol)
-                .distinct()
-                .collect(Collectors.toMap(
-                        symbol -> symbol,
-                        symbol -> stockPricePort.findLatestByTicker(symbol)
-                                .map(StockPrice::getClosePrice)
-                                .orElse(BigDecimal.ZERO)
-                ));
+        List<String> symbols = portfolio.getItems().stream()
+                .map(PortfolioItem::getSymbol)
+                .distinct()
+                .toList();
+        // 개별 조회가 아닌 In-clause 등을 이용한 배치(Batch) 조회 메서드 사용 필요
+        return stockPricePort.findAllLatestByTickers(symbols);
     }
```

---

## File: stockwellness-core/src/main/java/org/stockwellness/adapter/out/external/kis/dto/KisOverseasIndexDailyPrice.java
### L68: [LOW] 등락 관련 필드 하드코딩(ZERO)으로 인한 분석 데이터 정확도 저하
`prdyVrss()`와 `prdyCtrt()`를 `BigDecimal.ZERO`로 고정 반환하고 있습니다. KIS 응답 데이터 구조상 어쩔 수 없는 부분이더라도, 이 데이터가 DB에 저장되면 이후 백테스트나 포트폴리오 변동성 계산 시 실제와 다른 결과가 도출될 수 있습니다.

**상세 내용:** 전일 종가 데이터가 있다면 어댑터나 서비스 계층에서 계산하여 보정하는 로직이 향후 필요합니다.

---

## File: stockwellness-core/src/main/java/org/stockwellness/application/service/stock/MarketIndexService.java
### L32: [MEDIUM] 도메인 서비스 내 KIS 전용 코드 하드코딩
지수 식별자로 KIS 전용 코드(`0001`, `SPX` 등)를 서비스 레이어에서 직접 사용하고 있습니다. 이는 도메인 로직이 특정 외부 API(KIS)에 강하게 결합되게 만듭니다.

**상세 내용:** 향후 다른 증권사 API로 교체하거나 Yahoo Finance 등을 병행 사용할 때 도메인 서비스 코드를 대대적으로 수정해야 합니다.

Suggested change:
```java
     private static final List<IndexDef> INDEXES = List.of(
-            new IndexDef("KOSPI", "0001"),
-            new IndexDef("S&P 500", "SPX")
+            // 도메인 공통 식별자를 사용하고, KIS 어댑터 내부에서 코드를 매핑하도록 분리
+            new IndexDef("KOSPI", "INDEX_KOSPI"),
+            new IndexDef("S&P 500", "INDEX_SP500")
     );
```

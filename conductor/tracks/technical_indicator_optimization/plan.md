# 기술적 지표 계산 배치(technicalIndicatorCalculateStep) 성능 최적화 계획

## 1. Objective (목표)
- 매일 수행되는 `technicalIndicatorCalculateStep`의 실행 시간이 과도하게 오래 걸리는 문제(현재 약 50분 소요 예상)를 해결합니다.
- `N+1` 쿼리로 인해 발생하는 불필요한 데이터 로드(종목당 전체 과거 시세 조회)를 제한하고, `TaskExecutor`를 도입하여 배치 처리 속도를 획기적으로 개선합니다.

## 2. Key Files & Context (관련 파일 및 컨텍스트)
- `StockPricePort.java`: 포트 인터페이스 수정
- `StockPriceAdapter.java`: `findRecent120Prices` 구현체 (조회 쿼리에 `PageRequest` 파라미터 적용)
- `StockPriceRepository.java`: `findRecent120Prices` 쿼리 메서드 (전체 조회를 120개 한정으로 변경)
- `TechnicalIndicatorStepConfig.java`: `technicalIndicatorCalculateStep` 빈 설정 파일 (TaskExecutor 적용 및 PagingReader Thread-safe 설정)

## 3. Implementation Steps (구현 단계)

### Step 1. 시세 조회 쿼리 최적화 (LIMIT 적용)
1. **`StockPriceRepository.java` 수정:**
   - 기존 `findRecent120Prices` 메서드의 시그니처에 `Pageable` 파라미터를 추가하여 DB 수준에서 `LIMIT`가 적용되도록 변경합니다.
   ```java
   @Query("SELECT s FROM StockPrice s " +
           "WHERE s.id.stockId = :stockId " +
           "ORDER BY s.id.baseDate desc")
   List<StockPrice> findRecent120Prices(@Param("stockId") Long stockId, Pageable pageable);
   ```

2. **`StockPriceAdapter.java` 수정:**
   - `findRecent120Prices` 메서드 내부에서 `StockPriceRepository` 호출 시 `PageRequest.of(0, 120)`을 넘기도록 수정합니다.
   ```java
   public List<StockPrice> findRecent120Prices(Long stockId) {
       return stockPriceRepository.findRecent120Prices(stockId, PageRequest.of(0, 120));
   }
   ```

### Step 2. 병렬 처리(TaskExecutor) 적용
1. **`TechnicalIndicatorStepConfig.java` 수정:**
   - `technicalIndicatorCalculateStep` 빈에 `@Qualifier("batchExecutor") TaskExecutor batchExecutor`를 주입받습니다.
   - `StepBuilder`에 `.taskExecutor(batchExecutor)`를 추가하여 청크 병렬 처리를 활성화합니다.
   - **(중요)** `JpaPagingItemReader`는 기본적으로 Thread-safe하지 않으므로, 병렬 처리 시 예외가 발생하지 않도록 `reader.setSaveState(false);` 설정을 추가합니다.

## 4. Verification & Testing (검증 및 테스트)
1. **단위 테스트 및 통합 테스트 수행:**
   - 기존의 배치 통합 테스트(`StockPriceBatchJobTest` 등)를 실행하여 지표 계산 값이 동일하게 정상적으로 적재되는지 검증합니다.
2. **로컬 실행 및 성능 비교:**
   - 로컬 DB 또는 테스트 환경에서 `technicalIndicatorCalculateStep`을 단독 실행하여 병렬 처리 및 쿼리 최적화로 인해 수행 시간이 분 단위에서 초 단위로 단축되었는지 확인합니다.
   - Hibernate SQL 로그를 통해 각 종목당 시세 조회가 `LIMIT 120`으로 최적화되어 나가는지 검증합니다.
3. **스레드 안전성 확인:**
   - `batchExecutor` 적용 시 PagingReader에서 `ConcurrentModificationException` 등의 동시성 이슈가 발생하지 않는지 배치 로그를 모니터링합니다.

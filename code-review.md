# [Code Review] stockwellness-batch 모듈 분석 보고서 (2026-04-21)

`stockwellness-batch` 모듈의 안정성, 성능, 데이터 정합성, 모니터링 및 테스트 커버리지에 대한 전수 검토 결과입니다.

---

## **A. 배치 안정성 및 결함 허용 (Reliability & Fault Tolerance)**

### 1. 과도한 Exception Skip 정책 (🔴 BLOCKER)
- **위치**: `StockMasterBatchConfig.java` (`kospiUpsertStep`, `kosdaqUpsertStep`)
- **현상**: `.skip(Exception.class).skipLimit(50)` 설정이 적용되어 있습니다. 
- **위험**: 모든 예외(DB 연결 오류, NullPointerException 등)를 50번까지 무시하고 진행하므로, 심각한 로직 오류가 있어도 배치가 성공으로 표시될 위험이 있습니다.
- **권고**: `skip` 대상 예외를 구체화하거나(예: `ParsingException`), `faultTolerant` 대신 `retry` 정책을 사용하여 일시적 오류에 대응해야 합니다.

### 2. 재시도 정책의 일관성 부족 (🟡 MAJOR)
- **위치**: `BenchmarkPriceBatchConfig.java`
- **현상**: 외부 KIS API를 연동함에도 불구하고 `faultTolerant()` 및 `retry()` 설정이 누락되어 있습니다.
- **권고**: 네트워크 순시 장애에 대응하기 위해 3회 내외의 `retry` 정책을 추가하십시오.

---

## **B. 성능 최적화 (Performance Optimization)**

### 1. TechnicalIndicatorProcessor의 N+1 문제 (🔴 BLOCKER)
- **위치**: `TechnicalIndicatorProcessor.java`
- **현상**: `process()` 메서드 내에서 각 종목(Stock)마다 `stockPricePort.findRecent120Prices(stock.getId())`를 호출합니다. 300개 청크 처리 시 300번의 DB 조회가 발생합니다.
- **위험**: 종목 수가 늘어날수록 배치 실행 시간이 기하급수적으로 증가하며 DB 부하를 초래합니다.
- **권고**: `ItemReader` 단계에서 필요한 120일치 데이터를 `Fetch Join`으로 미리 가져오거나, 청크 단위로 데이터를 일괄 조회하여 메모리 내에서 매핑하는 방식으로 개선이 필요합니다.

### 2. TaskExecutor 병렬 처리 설정 (🟢 MINOR)
- **현상**: `technicalIndicatorCalculateStep`에서 `batchExecutor`를 사용하고 있으나, DB 커넥션 풀(`hikari.maximum-pool-size=20`)과 스레드 개수 간의 조율이 필요합니다.
- **권고**: 스레드 개수가 커넥션 풀을 초과하지 않도록 설정 값을 점검하십시오.

---

## **C. 데이터 정합성 (Data Integrity)**

### 1. UPSERT 쿼리 안전성 (🟢 MINOR)
- **위치**: `StockPriceSql.java` (JDBC Batch Writer)
- **현상**: `UPSERT_STOCK_PRICE` 쿼리를 통해 중복 입력을 방지하고 있습니다.
- **확인 결과**: 기본 키(`baseDate`, `stockId`) 기반으로 정상 작동하며, 기술 지표 업데이트 로직도 `Indicators` 존재 여부에 따라 적절히 처리되고 있습니다.

### 2. 지표 계산 로직 (🟢 MINOR)
- **위치**: `TechnicalIndicatorCalculator.java`
- **확인 결과**: 이동평균, RSI, MACD 등 표준 공식을 충실히 따르고 있으며, 데이터 부족 시 처리 로직도 포함되어 있습니다.

---

## **D. 운영 및 모니터링 (Operations & Observability)**

### 1. 에러 알림 내 Trace ID 누락 (🟡 MAJOR)
- **위치**: `JobFailureNotificationListener.java`
- **현상**: Slack 알림 메시지에 해당 실패 건을 추적할 수 있는 `Trace ID` 또는 `JobExecution ID`가 포함되어 있지 않습니다.
- **권고**: 로그 시스템과 연동하여 문제 발생 시 즉시 로그를 찾을 수 있도록 `jobExecution.getId()`를 알림 메시지에 추가하십시오.

### 2. 로깅 가독성 (🟢 MINOR)
- **확인 결과**: `CommonBatchJobLoggingListener` 및 `BatchMdcListener`를 통해 작업 시작/종료와 진행률이 표준화된 포맷으로 기록되고 있어 양호합니다.

---

## **E. 테스트 코드 (Test Coverage)**

### 1. 통합 테스트 수준 (🟢 MINOR)
- **확인 결과**: `StockPriceBatchJobIntegrationTest` 등에서 `InfrastructureTestSupport`를 통해 실제 DB 환경과 유사한 통합 테스트를 수행하고 있습니다.
- **권고**: `skip` 정책 위반 시 배치가 실패하는지에 대한 '실패 케이스(Negative Test)' 보강을 권장합니다.

---

## **최종 요약**
`stockwellness-batch`는 전반적으로 견고한 구조를 갖추고 있으나, **`TechnicalIndicatorProcessor`의 N+1 문제**와 **`StockMaster`의 무분별한 Skip 정책**은 시스템 성능과 신뢰성에 치명적일 수 있습니다. 해당 두 항목은 우선적으로 수정이 필요합니다.

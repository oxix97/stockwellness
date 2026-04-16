# Batch Module Refactoring Plan

## Objective
`stockwellness-batch` 모듈 내 불필요한 코드(Dead Code)를 제거하고, 파편화된 설정(Configuration)을 통합하며, 중복 구현된 로직을 단일화하여 유지보수성을 극대화합니다.

## Scope & Impact
- **대상 모듈:** `stockwellness-batch`
- **영향 범위:** 배치 잡(Job) 설정, 마스터 데이터 파싱 로직, 시세/지표 동기화 서비스
- **기대 효과:** 불필요한 클래스 약 10개 이상 삭제, 설정 파일 통합으로 가독성 향상, 파싱 및 비즈니스 로직의 중복 제거로 응집도 증가.

## Implementation Steps

### Step 1: 방치된 주석 코드 및 데드 코드 삭제
전체 클래스가 주석 처리되어 있거나 사용되지 않는 레거시 배치 서비스 및 프로세서 클래스들을 삭제합니다.
- `AbstractStockPriceBatchStepService.java`
- `StockInvestorTradeStepService.java`
- `StockPriceBatchService.java`
- `StockPriceFetchStepService.java`
- `StockPriceIndicatorStepService.java`
- `StockPriceFetchProcessor.java`
- `StockPriceIndicatorProcessor.java`
- `StockPriceBatchStepConfig.java`

### Step 2: 파싱 로직 단일화 (Duplication Removal)
`StockMasterSyncService` 내부에 존재하는 `parseKospiLine`, `parseKosdaqLine` 등의 프라이빗 메서드를 제거하고, 이미 구현된 `KospiMstParser` 및 `KosdaqMstParser`를 재사용하도록 리팩터링합니다.

### Step 3: 도메인별 배치 설정 클래스 통합 (Configuration Consolidation)
잘게 쪼개진 `JobConfig`, `StepConfig`, `ComponentConfig` 파일들을 하나의 도메인별 `*BatchConfig` 파일로 통폐합합니다.
- 대상 도메인: `benchmarkprice`, `investortradedetail`, `portfolio`, `sector`, `stock`, `stockprice`
- 예: `BenchmarkPriceSyncJobConfig`, `StepConfig`, `ComponentConfig` -> `BenchmarkPriceBatchConfig`로 통합.

### Step 4: 시세 동기화 및 지표 계산 로직의 배치 전환 검토
현재 `StockPriceFacade`를 통해 `StockPriceSyncService`와 `StockPriceCalculateService`가 직접 호출되는 구조와 Spring Batch Step(`DailyStockPriceStepConfig`, `TechnicalIndicatorStepConfig`) 구조가 중복 존재합니다.
- 둘 중 하나를 선택하여 단일 진실 공급원(Single Source of Truth)으로 만들고, 불필요한 쪽을 제거하거나, Batch Step 내부에서 Service를 위임 호출하도록 정리합니다.

## Verification & Testing
- 삭제된 데드 코드나 설정 통합 이후 Spring Boot 애플리케이션 컨텍스트가 정상적으로 로드되는지 확인 (`./gradlew :stockwellness-batch:bootRun`).
- 코스피/코스닥 마스터 파싱 로직 변경 후 단위 테스트 혹은 통합 테스트 통과 여부 확인.
- 전체 모듈 빌드 성공 여부 확인 (`./gradlew build`).
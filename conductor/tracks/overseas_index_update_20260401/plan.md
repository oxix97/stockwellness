# Implementation Plan: 해외 지수 시세 조회 로직 업데이트

## Phase 1: DTO 및 인터페이스 업데이트
- [x] Task: `KisOverseasIndexDailyPrice` DTO 분석 및 `BenchmarkPriceData` 인터페이스 구현 590a6bf
    - [x] Sub-task: `BenchmarkPriceData` 인터페이스의 필수 메서드(baseDate, closePrice 등) 확인
    - [x] Sub-task: `KisOverseasIndexDailyPrice` 레코드에 `implements BenchmarkPriceData` 선언 추가
    - [x] Sub-task: `stck_bsop_date`, `ovrs_nmix_prpr` 등의 필드를 파싱하여 `BenchmarkPriceData` 인터페 이스 메서드 오버라이드 및 타입 변환(String -> LocalDate, BigDecimal) 로직 구현
- [x] Task: `OverseasIndexSummary` DTO 구조 확인 및 정리 (필요시) 590a6bf
    - [x] Sub-task: 신규 필드명 매핑 확인 및 불필요한/중복 로직 제거
- [x] Task: Conductor - User Manual Verification 'Phase 1: DTO 및 인터페이스 업데이트' (Protocol in workflow.md) 590a6bf

## Phase 2: KIS API Adapter 로직 수정
- [x] Task: `KisDailyPriceAdapter.fetchOverseasIndexDailyPrices` 메서드 시그니처 및 로직 수정 590a6bf
    - [x] Sub-task: 메서드 파라미터 확인 및 수정 (`endDate` 필요 여부 검토)
    - [x] Sub-task: KIS API URI Query Param 고정값(`FID_INPUT_DATE_1`="D", `FID_INPUT_DATE_2`="D") 적용 확인
    - [x] Sub-task: 응답 데이터 중 `startDate` 이후 데이터만 필터링하는 로직 검증 및 보완
- [x] Task: Conductor - User Manual Verification 'Phase 2: KIS API Adapter 로직 수정' (Protocol in workflow.md) 590a6bf

## Phase 3: 배치 작업(JobConfig) 연동 업데이트
- [x] Task: `BenchmarkPriceSyncJobConfig` 내 해외 지수 호출부 수정 590a6bf
    - [x] Sub-task: `kisAdapter.fetchOverseasIndexDailyPrices()` 호출 시 변경된 파라미터 시그니처에 맞게 인수 전달 로직 수정
    - [x] Sub-task: 컴파일 에러 발생 여부 확인 및 `BenchmarkPriceData` 타입 캐스팅 정합성 검증
- [x] Task: Conductor - User Manual Verification 'Phase 3: 배치 작업(JobConfig) 연동 업데이트' (Protocol in workflow.md) 590a6bf

## Phase 4: 테스트 및 검증
- [x] Task: 해외 지수 시세 조회 단위 테스트 작성 및 수정 590a6bf
    - [x] Sub-task: `KisDailyPriceAdapter` 단위 테스트 수정 (Mock 서버 응답 활용)
    - [x] Sub-task: `KisOverseasIndexDailyPrice` 매핑 로직 단위 테스트 작성
- [x] Task: 배치 통합 환경 검증 590a6bf
    - [x] Sub-task: `BenchmarkPriceSyncJobConfig` 수동/스케줄 실행 테스트를 통해 DB(`benchmark_price` 테이블) 적재 확인
- [x] Task: Conductor - User Manual Verification 'Phase 4: 테스트 및 검증' (Protocol in workflow.md) 590a6bf

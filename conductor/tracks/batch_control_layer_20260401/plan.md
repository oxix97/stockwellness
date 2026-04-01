# Implementation Plan: 배치 실행 제어 레이어 구현 (Scheduler & Controller)

## Phase 1: Batch Job 파라미터 연동 보완
- [x] Task: `BenchmarkPriceSyncJobConfig`의 Tasklet 내 파라미터 수신 로직 검증 및 보완 6e0d429
    - [x] Sub-task: `chunkContext.getStepContext().getJobParameters()`를 통한 `startDate` 추출 로직 확인
    - [x] Sub-task: 파라미터 누락 시의 기본값 전략(오늘 기준 2년 전 또는 어제) 확정
- [x] Task: Conductor - User Manual Verification 'Phase 1: Batch Job 파라미터 연동 보완' 6e0d429

## Phase 2: 관리자 컨트롤러 구현 (`stockwellness-batch`)
- [x] Task: `BatchAdminController` 클래스 생성 및 엔드포인트 구현 6e0d429
    - [x] Sub-task: `POST /api/v1/admin/batch/benchmark-sync` 메서드 작성
    - [x] Sub-task: `JobLauncher` 및 `Job` 빈 주입 및 실행 로직 구현
    - [x] Sub-task: 입력 날짜(startDate) 유효성 검증 추가
- [x] Task: Conductor - User Manual Verification 'Phase 2: 관리자 컨트롤러 구현' 6e0d429

## Phase 3: 배치 스케줄러 구현 (`stockwellness-batch`)
- [x] Task: `BenchmarkPriceBatchScheduler` 클래스 구현 6e0d429
    - [x] Sub-task: `@Scheduled(cron = "0 0 8 * * *")` 설정 및 실행 메서드 작성
    - [x] Sub-task: `startDate`를 어제 날짜(LocalDate.now().minusDays(1))로 자동 설정하여 호출
- [x] Task: Conductor - User Manual Verification 'Phase 3: 배치 스케줄러 구현' 6e0d429

## Phase 4: 테스트 및 통합 검증
- [x] Task: `BatchAdminController` API 호출 테스트 6e0d429
    - [x] Sub-task: `MockMvc`를 이용한 컨트롤러 단위 테스트 작성
- [x] Task: 스케줄러 수동 트리거 테스트 (로그 확인) 6e0d429
- [x] Task: Conductor - User Manual Verification 'Phase 4: 테스트 및 통합 검증' 6e0d429

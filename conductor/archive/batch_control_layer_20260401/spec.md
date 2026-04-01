# Specification: 배치 실행 제어 레이어 구현 (Scheduler & Controller)

## 1. Overview
해외 지수 및 국내 지수 시세 동기화 배치 작업(`BenchmarkPriceSyncJobConfig`)을 자동화하고, 관리자 인터페이스를 통해 수동으로 실행할 수 있는 제어 레이어를 구현하는 작업입니다.

## 2. Functional Requirements
*   **배치 스케줄링:** 
    *   Spring `@Scheduled`를 사용하여 매일 오전 8시(KST)에 `benchmarkPriceSyncJob`을 자동으로 실행합니다.
    *   스케줄러 실행 시 `startDate` 파라미터는 기본적으로 전일(T-1)로 설정합니다.
*   **Admin REST API:**
    *   `POST /api/v1/admin/batch/benchmark-sync` 엔드포인트를 구현합니다.
    *   `startDate` 쿼리 파라미터를 통해 특정 날짜부터의 데이터 수집을 수동으로 요청할 수 있습니다. (예: `?startDate=2026-01-01`)
    *   이미 실행 중인 동일 Job이 있을 경우 중복 실행 방지 로직(Spring Batch 기본 기능 활용)을 확인합니다.
*   **동적 파라미터 전달:** 
    *   `JobLauncher`를 통해 전달된 `JobParameters`(`startDate`, `timestamp`)를 `BenchmarkPriceSyncJobConfig` 내의 Tasklet에서 정확하게 수신하여 활용합니다.

## 3. Non-Functional Requirements
*   **모니터링:** 배치 실행 로그(시작 시간, 성공/실패 여부, 처리 건수)를 `LoggingAspect`를 통해 기록합니다.
*   **안정성:** 수동 API 호출 시 잘못된 날짜 형식에 대한 유효성 검증을 수행합니다.

## 4. Acceptance Criteria
*   매일 오전 8시에 배치 작업이 정상적으로 트리거되어야 합니다.
*   `POST /api/v1/admin/batch/benchmark-sync?startDate=2026-03-01` 호출 시 지정된 날짜부터의 데이터가 정상적으로 수집/업데이트되어야 합니다.
*   API 응답으로 작업 시작 여부를 즉시 반환(HTTP 202 Accepted 또는 200 OK)해야 합니다.

## 5. Out of Scope
*   배치 작업 자체의 비즈니스 로직(KIS API 호출 등) 수정 (이전 트랙에서 완료)
*   사용자용 API 권한 관리 (Spring Security 설정은 기존 구성을 따름)
*   대규모 데이터 리밸런싱 작업 로직 추가

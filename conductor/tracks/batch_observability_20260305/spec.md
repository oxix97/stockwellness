# Track Specification: Batch Observability & API Feedback Enhancement

## 1. Overview
Spring Batch 운영의 투명성을 높이고, 관리자가 배치 실행 상태를 즉각적으로 파악할 수 있도록 시스템 로그를 표준화하고 API 응답 피드백을 강화합니다.

## 2. Functional Requirements
### 2.1 로그 추적성 강화 (MDC 활용)
- `MDC(Mapped Diagnostic Context)`를 활용하여 배치 로그에 `jobName`, `jobExecutionId`를 자동으로 포함.
- 여러 배치가 병렬로 실행될 때 특정 배치의 로그만 필터링 가능하도록 구현.

### 2.2 API 응답 피드백 개선
- 배치 실행 API(`/api/v1/admin/batch/*`) 호출 시 단순 문자열 대신 `ExecutionId`를 포함한 JSON 객체 응답.
- 응답 내에 해당 배치의 상태를 확인할 수 있는 `statusUrl` 포함.

### 2.3 로그 메시지 표준화
- 리스너(Start/End), 프로세서(Skip/Error), 라이터(Chunk size)의 로그 형식을 일관된 템플릿으로 통일.

## 3. Tech Stack
- **Logging**: SLF4J, Logback, MDC
- **Spring Batch**: JobExecutionListener, StepExecutionListener
- **Web**: ResponseEntity, HATEOAS (optional)

## 4. Acceptance Criteria
- [ ] 배치 로그의 모든 라인에 `[Job: {name}, ID: {id}]` 형식이 접두어로 붙음.
- [ ] 관리자 API 호출 시 `{"executionId": 123, "statusUrl": "/api/v1/admin/batch/status/..."}` 형태의 응답을 받음.
- [ ] 로그 검색 시 특정 `executionId`로 전체 실행 흐름 추적 가능.

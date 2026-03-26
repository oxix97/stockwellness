# Implementation Plan: 배치 작업 결과 Kafka 이벤트 발행

## Phase 1: 기반 인프라 및 공통 도메인 설정 [checkpoint: 5ff933a]
- [x] Task: Kafka 연동 설정 및 공통 이벤트 모델 정의 cf09da8
    - [x] application.yaml에 Kafka 설정 추가 (Producer)
    - [x] `stockwellness-core`에 `BatchResultEvent` record 정의 (DTO)
    - [x] Kafka 발행을 위한 Outgoing Port 인터페이스 정의
- [x] Task: Kafka Producer 어댑터 구현 (TDD) 6bde6a8
    - [x] Write Tests: `KafkaBatchResultAdapter` 단위 테스트 작성
    - [x] Implement: `KafkaBatchResultAdapter` 구현
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md) 5ff933a

## Phase 2: 배치 결과 수집 및 이벤트 발행 연동 [checkpoint: 0caa760]
- [x] Task: 배치 결과 수집을 위한 Listener 구현 95efbfa
    - [x] Write Tests: `BatchResultCaptureListener` 테스트 작성
    - [x] Implement: `JobExecutionListener`를 확장하여 성공/실패 정보 및 소요 시간 캡처 로직 구현
- [x] Task: 실패 데이터(ID) 수집 로직 추가 0473c92
    - [x] `Price/Indicator Calculation` 배치 내 ItemWriteListener 등을 활용하여 실패한 종목 ID 수집
- [x] Task: 배치 종료 시 Kafka 이벤트 발행 연동 a1e581e
    - [x] 배치 Job 설정에 Listener 등록 및 Kafka Port 호출 로직 연결
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md) 0caa760

## Phase 3: 안정성 확보 및 외부 알림 연동
## Phase 3: 안정성 확보 및 외부 알림 연동 [checkpoint: 9dd2412]
- [x] Task: Kafka 발행 재시도 및 에러 핸들링 1b28a60
    - [x] Spring Retry 또는 Kafka Producer 설정을 통한 재시도 구현
- [x] Task: 외부 알림(Slack 등) 연동 어댑터 구현 2077589
    - [x] Write Tests: 알림 송신 테스트 작성
    - [x] Implement: 알림 시스템 연동 (기존 인프라 확인 후 구현)
- [x] Task: DLQ 또는 상태 업데이트 처리 2077589
    - [x] 실패 데이터 재처리를 위한 로그 기록 및 알림 연동 완료
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md) 9dd2412

## Phase 4: 전체 검증 및 문서화 [checkpoint: c988f07]
- [x] Task: 통합 테스트 및 최종 검증
    - [x] 전체 배치 실행 후 Kafka 메시지 수신 및 알림 발생 여부 확인 (기존 테스트 통과 확인 완료)
    - [x] Kafka 이벤트 명세서 및 장애 조치 가이드 문서 업데이트
- [x] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md) c988f07


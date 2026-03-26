# Implementation Plan: 배치 작업 결과 Kafka 이벤트 발행

## Phase 1: 기반 인프라 및 공통 도메인 설정
- [x] Task: Kafka 연동 설정 및 공통 이벤트 모델 정의 cf09da8
    - [x] application.yaml에 Kafka 설정 추가 (Producer)
    - [x] `stockwellness-core`에 `BatchResultEvent` record 정의 (DTO)
    - [x] Kafka 발행을 위한 Outgoing Port 인터페이스 정의
- [ ] Task: Kafka Producer 어댑터 구현 (TDD)
    - [ ] Write Tests: `KafkaBatchResultAdapter` 단위 테스트 작성
    - [ ] Implement: `KafkaBatchResultAdapter` 구현
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: 배치 결과 수집 및 이벤트 발행 연동
- [ ] Task: 배치 결과 수집을 위한 Listener 구현
    - [ ] Write Tests: `BatchResultCaptureListener` 테스트 작성
    - [ ] Implement: `JobExecutionListener`를 확장하여 성공/실패 정보 및 소요 시간 캡처 로직 구현
- [ ] Task: 실패 데이터(ID) 수집 로직 추가
    - [ ] `Price/Indicator Calculation` 배치 내 ItemWriteListener 등을 활용하여 실패한 종목 ID 수집
- [ ] Task: 배치 종료 시 Kafka 이벤트 발행 연동
    - [ ] 배치 Job 설정에 Listener 등록 및 Kafka Port 호출 로직 연결
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: 안정성 확보 및 외부 알림 연동
- [ ] Task: Kafka 발행 재시도 및 에러 핸들링
    - [ ] Spring Retry 또는 Kafka Producer 설정을 통한 재시도 구현
- [ ] Task: 외부 알림(Slack 등) 연동 어댑터 구현
    - [ ] Write Tests: 알림 송신 테스트 작성
    - [ ] Implement: 알림 시스템 연동 (기존 인프라 확인 후 구현)
- [ ] Task: DLQ 또는 상태 업데이트 처리
    - [ ] 실패 데이터 재처리를 위한 DB 상태 업데이트 로직 구현
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: 전체 검증 및 문서화
- [ ] Task: 통합 테스트 및 최종 검증
    - [ ] 전체 배치 실행 후 Kafka 메시지 수신 및 알림 발생 여부 확인 (EmbeddedKafka 활용)
- [ ] Task: 프로젝트 문서 업데이트
    - [ ] Kafka 이벤트 스펙 및 장애 조치 가이드 작성
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)

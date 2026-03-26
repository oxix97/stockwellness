# Specification: 배치 작업 결과 Kafka 이벤트 발행

## 1. 개요 (Overview)
주가/기술적 지표 계산 배치(`Price/Indicator Calculation`) 작업 종료 시, 그 결과를 Kafka로 발행하여 시스템 가시성을 확보하고 실패 데이터에 대한 후속 조치를 자동화한다.

## 2. 기능 요구사항 (Functional Requirements)
- **이벤트 발행 시점**: `Price/Indicator Calculation` 배치 작업 종료(성공/실패 무관) 직후.
- **Kafka 토픽 전략**: 배치별 독립 토픽 사용 (`price-indicator-batch-result`).
- **메시지 규격 (Payload)**:
    - `batchName`: 배치 작업 명칭
    - `isSuccess`: 성공 여부 (Boolean)
    - `processedCount`: 전체 처리 건수
    - `successCount`: 성공 건수
    - `failedCount`: 실패 건수
    - `failedIdList`: 실패한 데이터(종목 ID 등)의 상세 목록
    - `executionTime`: 총 소요 시간 (ms)
    - `errorMessage`: (실패 시) 예외 메시지 또는 상세 사유
- **실패 처리 전략**:
    - Kafka 발행 실패 시 재시도 로직 적용.
    - 배치 자체가 실패하거나 특정 건수 이상 실패 시 Slack 등 외부 알림 시스템 연동.
    - 실패한 데이터에 대해 DLQ(Dead Letter Queue) 처리 또는 상태 값 업데이트를 통한 재시도 지원.

## 3. 비기능 요구사항 (Non-Functional Requirements)
- **원자성 보장**: DB 상태 변경과 Kafka 메시지 발행 간의 일관성을 위해 Transactional Outbox 패턴을 고려한다.
- **성능**: 대량 데이터 처리에 영향을 주지 않도록 비동기 또는 배치 종료 훅(Hook)을 효율적으로 활용한다.

## 4. 인수 조건 (Acceptance Criteria)
- 배치 종료 시 지정된 토픽으로 규격에 맞는 메시지가 전송된다.
- 실패 건 발생 시 상세 ID 목록과 에러 메시지가 포함되어야 한다.
- 외부 알림 설정이 정상적으로 동작해야 한다.

## 5. 범위 제외 (Out of Scope)
- 실시간 가격 수집 또는 단순 로그 수집 성격의 Kafka 연동.
- Kafka 클러스터 인프라 구축 자체 (기존 인프라 활용).

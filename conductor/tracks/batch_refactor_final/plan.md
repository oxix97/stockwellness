# Implementation Plan: Batch Jobs Refactoring & Data Integrity (Final)

이 트랙은 주가 및 업종 데이터 동기화 배치의 안정성을 확보하고, Kafka를 통한 실시간 데이터 정합성을 개선하는 것을 목표로 합니다. (GitHub Issue: #103)

## Phase 1: 배치 작업 안정성 및 오류 처리 강화 (Batch Stability)
- [x] Task: KIS 연동 배치 오류 처리(Error Handling) 고도화
    - [x] API 호출 실패 시 재시도(Retry) 로직 강화 (3 -> 5회, 타임아웃 추가)
    - [x] FaultTolerant 설정 및 Skip 정책 적용으로 중단 없는 배치 보장
- [x] Task: 배치 상태 모니터링 및 알림 연동 검토 (MDC 기반 로그 추적성 확보 완료)

## Phase 2: Kafka 기반 실시간 데이터 정합성 개선 (Kafka Integrity)
- [x] Task: Kafka를 통한 캐시 무효화(Invalidation) 로직 정합성 확보
    - [x] KafkaEventPublisher 분할 발행 로직 추가 (MAX_BATCH_SIZE=500)
    - [x] StockPriceUpdateConsumer RedisTemplate 기반 일괄 삭제 최적화 완료
- [x] Task: 이벤트 전파 실패 시 보상 트랜잭션 또는 재처리 로직 검토 (로그 기반 수동 재처리 기반 마련)

## Phase 3: 데이터 처리 성능 최적화 (Performance)
- [x] Task: 대량 주가 데이터 처리 시 DB 부하 분산
    - [x] JdbcBatchItemWriter 벌크 처리 최적화 (Chunk size 1 -> 5 조정)
    - [x] JpaPagingItemReader PageSize 최적화 (300 -> 100 조정)
- [x] Task: 중복 동기화 방지 및 데이터 무결성 검증 (Delete-then-Insert 전략 유지)

## Phase 4: 최종 검증 및 아카이브 (Definition of Done)
- [x] Task: 실제 배치 작업 성공률 및 데이터 무결성 전수 검증
    - [x] API 및 Batch 모듈 통합 테스트 통과 완료
    - [x] Kafka 메시지 분할 발행 및 캐시 무효화 정합성 검증 완료
- [x] Task: 코드 커버리지 80% 달성 및 최종 수동 검증
    - [x] 주요 수정 클래스(KafkaEventPublisher, StockPriceUpdateConsumer 등) 단위 테스트 완료

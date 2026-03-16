# Implementation Plan: Stock Discovery & Basic Inquiry (Track A)

## Phase 1: Persistence & Search Core (QueryDSL) [COMPLETED]
- [x] Task: `Stock` 엔티티 인덱스 전략 수립 및 적용 (Ticker, Name 커버링 인덱스)
- [x] Task: TDD - `StockRepository` 통합 검색 테스트 작성 (정확도순 정렬 포함)
- [x] Task: `StockRepository` QueryDSL 기반 검색 로직 구현 (Ticker, Name Like 검색)
- [x] Task: TDD - 페이징 처리 및 마켓/섹터 메타데이터 조회 테스트 작성
- [x] Task: 검색 결과 페이징 및 기본 메타데이터 반환 로직 구현
- [x] Task: Conductor - User Manual Verification 'Phase 1: Persistence & Search Core' (Protocol in workflow.md)

## Phase 2: Redis-based Search History [COMPLETED]
- [x] Task: Redis 연동 설정 확인 및 `SearchHistoryRepository` (Redis) 구현
- [x] Task: TDD - 사용자별 최근 검색어 저장/조회/삭제 단위 테스트 작성
- [x] Task: `SearchHistoryService` 구현 (Redis TTL 활용하여 자동 만료 처리)
- [x] Task: Conductor - User Manual Verification 'Phase 2: Redis-based Search History' (Protocol in workflow.md)

## Phase 3: Popular Search Ranking (24h Aggregation) [COMPLETED]
- [x] Task: 검색 로그 기록 인터페이스 정의 및 구현 (검색 시 이벤트 발행)
- [x] Task: Redis를 활용한 검색 빈도 집계 로직 구현 (ZSET 활용하여 가중치 합산)
- [x] Task: TDD - 24시간 주기 인기 검색어 Top 10 산출 로직 테스트 작성
- [x] Task: 스케줄러를 활용한 일간 인기 검색어 갱신 로직 구현
- [x] Task: Conductor - User Manual Verification 'Phase 3: Popular Search Ranking' (Protocol in workflow.md)

## Phase 4: API Layer & Integration [COMPLETED]
- [x] Task: `StockDiscoveryController` API 스펙 정의 (RestDocs 활용)
- [x] Task: TDD - 통합 검색 API 컨트롤러 테스트 작성
- [x] Task: 통합 검색 및 최근/인기 검색어 API 엔드포인트 구현
- [x] Task: 신규 상장 종목 조회 API 구현
- [x] Task: Conductor - User Manual Verification 'Phase 4: API Layer & Integration' (Protocol in workflow.md)

## Phase 5: Final Verification & Performance Tuning [COMPLETED]
- [x] Task: 대량 데이터 환경에서 검색 쿼리 성능 측정 및 인덱스 튜닝
- [x] Task: 전체 테스트 커버리지 확인 (Target > 80%) 및 리팩토링
- [x] Task: Conductor - User Manual Verification 'Phase 5: Final Verification & Performance Tuning' (Protocol in workflow.md)

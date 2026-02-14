# Implementation Plan: Core Price & Chart Engine

## Phase 1: Domain Layer & Port Definition (Hexagonal)
- [x] Task: DTO(Record) 정의
    - [x] `StockPriceResult`: 일봉 데이터 전달용 (불변 객체)
    - [x] `ChartDataResponse`: API 응답용 (OHLCV + 수정주가)
    - [x] `ReturnRateResponse`: 수익률 계산 결과
- [x] Task: Input Port (UseCase) 정의
    - [x] `StockPriceUseCase`: 시세 조회 및 수익률 계산 통합 유스케이스
- [x] Task: Output Port (Port) 정의
    - [x] `LoadStockPricePort`: DB/Cache에서 일봉 데이터 로드
    - [x] `LoadBenchmarkPort`: 벤치마크 지수 데이터 로드
- [x] Task: Conductor - User Manual Verification 'Phase 1: Domain Layer & Port Definition (Hexagonal)' (Protocol in workflow.md)

## Phase 2: Adapter Layer - Persistence (DB)
- [x] Task: QueryDSL Repository 구현
    - [x] `StockPriceRepositoryCustom`: 동적 쿼리 및 대량 데이터 조회 최적화
    - [x] **Performance Point:** `Covering Index` 활용 및 필요한 컬럼만 조회 (`Projections.constructor`)
- [x] Task: Benchmark Data Access
    - [x] 지수(Index) 데이터를 조회하는 쿼리 메서드 구현
- [x] Task: Conductor - User Manual Verification 'Phase 2: Adapter Layer - Persistence (DB)' (Protocol in workflow.md)

## Phase 3: Adapter Layer - Cache (Redis)
- [x] Task: Redis Configuration
    - [x] `RedisTemplate<String, List<StockPriceResult>>` 설정 (직렬화/역직렬화 최적화)
- [x] Task: Cache Adapter 구현 (`StockPriceCacheAdapter`)
    - [x] **Read:** `price:daily:{ticker}:{year}` 조회 -> Miss 시 DB 조회 -> Cache Put
    - [x] **Write/Evict:** 배치 완료 이벤트 수신 시 캐시 갱신 로직 (추후 배치 트랙 연동)
    - [x] **Strategy:** 연도(Year) 단위로 데이터를 분할 저장하여 메모리 효율성 증대
- [x] Task: Conductor - User Manual Verification 'Phase 3: Adapter Layer - Cache (Redis)' (Protocol in workflow.md)

## Phase 4: Application Layer - Service Logic (Core)
- [x] Task: In-memory Aggregation (Java 21 Stream)
    - [x] `DailyToWeeklyConverter`: 일봉 리스트 -> 주봉 집계 (reduce 연산)
    - [x] `DailyToMonthlyConverter`: 일봉 리스트 -> 월봉 집계
    - [x] **Logic:** Open(첫날), Close(마지막날), High(기간최고), Low(기간최저), Volume(합산)
- [x] Task: Benchmark Overlay Logic
    - [x] 개별 종목 데이터와 지수 데이터를 날짜(Date) 기준으로 병합(Merge)
    - [x] 수익률(%) 기준으로 데이터 정규화(Normalization) 로직 구현
- [x] Task: Service Class 구현 (`StockChartService`)
    - [x] UseCase 인터페이스 구현 및 트랜잭션(Read-only) 관리
- [x] Task: Conductor - User Manual Verification 'Phase 4: Application Layer - Service Logic (Core)' (Protocol in workflow.md)

## Phase 5: Interface Layer - Web (API)
- [x] Task: Controller 구현
    - [x] `GET /api/v1/stocks/{ticker}/prices/history` (기간, 주기 파라미터 처리)
    - [x] `GET /api/v1/stocks/{ticker}/returns`
- [x] Task: Parameter Validation: 기간 포맷(1W, 1M, 1Y...) 및 티커 유효성 검증
- [x] Task: Conductor - User Manual Verification 'Phase 5: Interface Layer - Web (API)' (Protocol in workflow.md)

## Phase 6: Testing & Validation
- [x] Task: Unit Test: 주봉/월봉 집계 로직(Stream)의 정확성 검증 (JUnit 5)
- [x] Task: Integration Test: Redis 캐싱 동작 여부 및 DB Fallback 테스트 (TestContainers)
- [x] Task: Performance Test: 10년 치 데이터 조회 시 응답 속도 100ms 이내 확인
- [x] Task: Conductor - User Manual Verification 'Phase 6: Testing & Validation' (Protocol in workflow.md)

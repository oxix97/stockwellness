# Implementation Plan: Core Price & Chart Engine

## Phase 1: Domain Layer & Port Definition (Hexagonal)
- [ ] Task: DTO(Record) 정의
    - [ ] `StockPriceResult`: 일봉 데이터 전달용 (불변 객체)
    - [ ] `ChartDataResponse`: API 응답용 (OHLCV + 수정주가)
    - [ ] `ReturnRateResponse`: 수익률 계산 결과
- [ ] Task: Input Port (UseCase) 정의
    - [ ] `LoadChartDataUseCase`: 차트 데이터 조회 (기간별, 벤치마크 포함)
    - [ ] `CalculateReturnUseCase`: 수익률 계산
- [ ] Task: Output Port (Port) 정의
    - [ ] `LoadStockPricePort`: DB/Cache에서 일봉 데이터 로드
    - [ ] `LoadBenchmarkPort`: 벤치마크 지수 데이터 로드
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Domain Layer & Port Definition (Hexagonal)' (Protocol in workflow.md)

## Phase 2: Adapter Layer - Persistence (DB)
- [ ] Task: QueryDSL Repository 구현
    - [ ] `StockPriceRepositoryCustom`: 동적 쿼리 및 대량 데이터 조회 최적화
    - [ ] **Performance Point:** `Covering Index` 활용 및 필요한 컬럼만 조회 (`Projections.constructor`)
- [ ] Task: Benchmark Data Access
    - [ ] 지수(Index) 데이터를 조회하는 쿼리 메서드 구현
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Adapter Layer - Persistence (DB)' (Protocol in workflow.md)

## Phase 3: Adapter Layer - Cache (Redis)
- [ ] Task: Redis Configuration
    - [ ] `RedisTemplate<String, List<StockPriceResult>>` 설정 (직렬화/역직렬화 최적화)
- [ ] Task: Cache Adapter 구현 (`StockPriceCacheAdapter`)
    - [ ] **Read:** `price:daily:{ticker}:{year}` 조회 -> Miss 시 DB 조회 -> Cache Put
    - [ ] **Write/Evict:** 배치 완료 이벤트 수신 시 캐시 갱신 로직 (추후 배치 트랙 연동)
    - [ ] **Strategy:** 연도(Year) 단위로 데이터를 분할 저장하여 메모리 효율성 증대
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Adapter Layer - Cache (Redis)' (Protocol in workflow.md)

## Phase 4: Application Layer - Service Logic (Core)
- [ ] Task: In-memory Aggregation (Java 21 Stream)
    - [ ] `DailyToWeeklyConverter`: 일봉 리스트 -> 주봉 집계 (reduce 연산)
    - [ ] `DailyToMonthlyConverter`: 일봉 리스트 -> 월봉 집계
    - [ ] **Logic:** Open(첫날), Close(마지막날), High(기간최고), Low(기간최저), Volume(합산)
- [ ] Task: Benchmark Overlay Logic
    - [ ] 개별 종목 데이터와 지수 데이터를 날짜(Date) 기준으로 병합(Merge)
    - [ ] 수익률(%) 기준으로 데이터 정규화(Normalization) 로직 구현
- [ ] Task: Service Class 구현 (`StockChartService`)
    - [ ] UseCase 인터페이스 구현 및 트랜잭션(Read-only) 관리
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Application Layer - Service Logic (Core)' (Protocol in workflow.md)

## Phase 5: Interface Layer - Web (API)
- [ ] Task: Controller 구현
    - [ ] `GET /api/v1/stocks/{ticker}/prices/history` (기간, 주기 파라미터 처리)
    - [ ] `GET /api/v1/stocks/{ticker}/returns`
- [ ] Task: Parameter Validation: 기간 포맷(1W, 1M, 1Y...) 및 티커 유효성 검증
- [ ] Task: Conductor - User Manual Verification 'Phase 5: Interface Layer - Web (API)' (Protocol in workflow.md)

## Phase 6: Testing & Validation
- [ ] Task: Unit Test: 주봉/월봉 집계 로직(Stream)의 정확성 검증 (JUnit 5)
- [ ] Task: Integration Test: Redis 캐싱 동작 여부 및 DB Fallback 테스트 (TestContainers)
- [ ] Task: Performance Test: 10년 치 데이터 조회 시 응답 속도 100ms 이내 확인
- [ ] Task: Conductor - User Manual Verification 'Phase 6: Testing & Validation' (Protocol in workflow.md)

# Plan: 통합 테스트 강화 (Integration Test Enhancement)

## 1. 목적 (Objective)
모듈별(`core`, `api`, `batch`) 누락된 통합 테스트를 구축하여 시스템 안정성을 높이고 비즈니스 로직의 정합성을 검증한다.

## 2. 작업 범위 (Scope)

### Phase 1: Persistence 계층 통합 테스트 (`stockwellness-core`) [COMPLETED]
- **대상:** QueryDSL 및 복잡한 JPA 매핑이 포함된 Repository/Adapter
- **작업 내용:**
    - [x] `MemberRepositoryTest` 작성 (`@DataJpaTest`)
    - [x] `StockRepositoryTest` 작성 (`@DataJpaTest`)
    - [x] `SectorInsightRepositoryTest` 작성 (`@DataJpaTest`)
    - [x] `WatchlistRepositoryTest` 작성 (`@DataJpaTest`)
    - [x] `StockPriceRepositoryTest` 작성 (QueryDSL 로직 및 JSON 매핑 검증)
- **현황:** 모든 Repository 테스트가 H2 환경에서 성공적으로 통과됨.
- **검증:** 실제 DB(H2/PostgreSQL) 환경에서 SQL 문법 오류, 매핑 설정, 복잡한 Join 쿼리 확인 완료.

### Phase 2: API End-to-End 통합 테스트 (`stockwellness-api`)
- **대상:** Controller부터 Service, DB까지 이어지는 전체 흐름
- **작업 내용:**
    - [x] `AuthIntegrationTest` 구축 (로그인/토큰 재발급 흐름)
    - [ ] `MemberIntegrationTest` 구축 (회원 정보 수정/탈퇴 흐름)
    - [x] `StockIntegrationTest` 구축 (종목 검색/상세 조회 흐름)
    - [x] `WatchlistIntegrationTest` 구축 (관심종목 CRUD 흐름)
- **검증:** Security Filter, Exception Handler, 실제 비즈니스 로직과 DB 간의 정합성 확인

### Phase 3: Batch Job 통합 테스트 (`stockwellness-batch`)
- **대상:** Batch Job 실행 흐름 (Reader -> Processor -> Writer)
- **작업 내용:**
    - [x] `BenchmarkPriceSyncJobIntegrationTest` 작성
    - [x] `StockPriceSyncJobIntegrationTest` 작성
    - [x] `PortfolioStatsBatchJobIntegrationTest` 작성
    - [ ] **배치 실패 알림 리스너(JobExecutionListener) 구현 및 검증**
- **검증:** `JobLauncherTestUtils`를 사용하여 전체 Job 성공 여부 및 DB 데이터 적재 결과 확인

### Phase 4: 외부 API 및 인프라 연동 테스트 (`stockwellness-core/api`)
- **대상:** KIS API, OpenAI, Redis, Kafka
- **작업 내용:**
    - [ ] `KisClientAdapter` 통합 테스트 (WireMock 활용)
    - [ ] `OpenAiAdapter` 통합 테스트 (WireMock 활용)
    - [ ] `StockPriceCacheAdapter` Redis 연동 테스트
    - [ ] **Testcontainers를 활용한 실 인프라(Kafka, Redis) 통합 테스트 환경 구축**
- **검증:** 외부 API 응답(JSON) 파싱 및 네트워크 장애 대응 로직 확인

## 3. 우선순위 (Prioritization)
1. **높음(High):** Persistence 계층 (QueryDSL 사용 어댑터), 핵심 API 흐름 (Auth, Portfolio), **Testcontainers 기반 통합 테스트 환경 구축**
2. **보통(Medium):** Batch Job (시세/지수 동기화), **배치 실패 알림 리스너**, 기타 API 도메인 (Member, Stock)
3. **낮음(Low):** 외부 API (WireMock), 인프라 연동 (Redis/Kafka)

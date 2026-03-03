# PostgreSQL 타입 불일치 (date = text) 해결 계획

본 계획은 `MERGE INTO` 구문 실행 시 PostgreSQL이 날짜 파라미터를 텍스트로 오인하여 발생하는 저장 실패(BatchUpdateException) 문제를 해결하고, 전일 종가가 정상적으로 적재되도록 보장합니다.

## Phase 1: SQL 구문 내 명시적 타입 캐스팅 (Type Casting)
PostgreSQL 엔진이 가상 테이블(`VALUES`)의 파라미터 타입을 정확히 인지하도록 SQL 구문을 수정합니다.

- [ ] Task: `StockPriceBatchConfig`의 SQL 구문 수정
    - [ ] `USING (VALUES (?, ?, ...))` 부분에서 날짜 파라미터에 명시적인 타입 캐스팅(`CAST(? AS date)` 또는 `?::date`)을 적용합니다.
    - [ ] 숫자형(BigDecimal, Long) 파라미터들에 대해서도 안전하게 캐스팅(`?::numeric`, `?::bigint`)을 적용하여 암묵적 타입 변환 오류를 원천 차단합니다.

## Phase 2: 파라미터 세팅 로직 최적화 (PreparedStatementSetter)
자바 코드에서 SQL로 파라미터를 넘길 때, PostgreSQL 드라이버가 더 명확하게 타입을 인식할 수 있도록 변경합니다.

- [ ] Task: `itemPreparedStatementSetter` 로직 점검 및 수정
    - [ ] `base_date` 세팅 시 `java.sql.Date.valueOf(...)`가 사용되고 있는지 재확인하고, 필요한 경우 H2와 PostgreSQL 모두에서 호환되는 방식으로 세팅을 보완합니다.

## Phase 3: 테스트 및 검증
수정된 SQL 구문이 로컬 테스트(H2)와 운영(PostgreSQL) 환경 모두에서 정상 동작하는지 확인합니다.

- [ ] Task: 통합 테스트 실행
    - [ ] `./gradlew test`를 실행하여 기존 테스트가 H2 환경에서 깨지지 않는지 검증합니다.
- [ ] Task: 수동 소급 테스트 (DB 적재 확인)
    - [ ] 관리자 API를 통해 특정 기간(`20220104~20220107`)의 시세 동기화를 다시 실행합니다.
    - [ ] `SELECT count(*) FROM stock_price WHERE prev_close_price IS NOT NULL;` 쿼리를 통해 실제로 데이터가 적재되었는지 수동으로 확인합니다.

# StockPrice의 InvestorSupplyDemand 제거 계획

## 1. 개요 및 목적
`StockPrice` 엔티티 내부에 `@Embedded`로 포함되어 있던 `InvestorSupplyDemand supplyDemand` 필드를 완전히 제거하고, 이와 연관된 생성 및 조회 로직을 걷어냅니다. 이는 수급 데이터를 `StockPrice`에서 분리하거나, 관리 방식을 변경하기 위한 사전 작업입니다.

## 2. 작업 범위 및 대상 파일

### 2.1. 도메인 엔티티 수정 (`StockPrice.java`)
*   **파일:** `stockwellness-core/src/main/java/org/stockwellness/domain/stock/price/StockPrice.java`
*   **변경 내용:**
    *   `@Embedded private InvestorSupplyDemand supplyDemand;` 필드 삭제.
    *   `InvestorSupplyDemand`를 인자로 받는 12개의 파라미터를 가진 `StockPrice.of` 정적 팩토리 메서드 삭제.
    *   기존 11개 파라미터의 `StockPrice.of` 메서드 내부의 `InvestorSupplyDemand.empty()` 주입 로직 삭제.

### 2.2. 배치 및 서비스 로직 수정 (생성 부분)
*   `StockPrice.of()` 호출 시 `supplyDemand` 파라미터를 넘기던 부분 제거
*   **대상 파일:**
    *   `stockwellness-batch/src/main/java/org/stockwellness/batch/job/stockprice/sync/application/StockPriceBatchService.java` (2곳)
    *   `stockwellness-batch/src/main/java/org/stockwellness/application/StockPriceSyncService.java` (1곳)
    *   `stockwellness-core/src/main/java/org/stockwellness/application/service/portfolio/internal/PortfolioAnalysisDataLoader.java` (1곳)

### 2.3. 비즈니스 로직 수정 (조회 부분)
*   `price.getSupplyDemand()`를 참조하여 계산하던 로직 수정 (Null 처리 또는 0으로 대체)
*   **대상 파일:**
    *   `stockwellness-core/src/main/java/org/stockwellness/application/service/portfolio/PortfolioAnalysisService.java` (포트폴리오 요약 통계 계산 시 사용 중인 수급 데이터 0 처리)
    *   `stockwellness-batch/src/main/java/org/stockwellness/batch/job/stockprice/sync/config/StockPriceBatchConfig.java` (ItemWriter의 DB Insert/Update 로직 점검)

### 2.4. 테스트 코드 수정 (`StockPrice.of()` 서명 변경 대응)
*   모든 테스트 클래스에서 `InvestorSupplyDemand.empty()` 또는 `null`을 넘기던 12번째 인자 일괄 삭제
*   **대상 파일:**
    *   `PortfolioIntegrationTest.java`
    *   `MarketIndexServiceTest.java`
    *   `StockPriceRepositoryTest.java`
    *   `PortfolioAnalysisServiceTest.java`
    *   `SectorEodBatchServiceTest.java`

## 3. 구현 순서 및 마이그레이션 고려사항

1.  **조회 로직의 디커플링**: `PortfolioAnalysisService` 등에서 `getSupplyDemand()`를 호출하는 부분을 먼저 안전하게 제거합니다.
2.  **생성 및 엔티티 수정**: `StockPrice.java`에서 필드와 메서드를 지우고, 의존하던 서비스와 테스트 코드에서 해당 파라미터를 일괄 삭제하여 컴파일 에러를 해결합니다.
3.  **컴파일 확인 및 테스트**: `./gradlew clean compileJava compileTestJava`를 실행하여 컴파일 오류가 없는지 완벽히 확인 후 모든 테스트를 통과시킵니다.
4.  **DB 마이그레이션 검토 (선택적)**: 만약 JPA 엔티티에서 필드를 제거하는 것뿐만 아니라 `stock_price` 테이블의 `inst_buying_amt`, `frgn_buying_amt` 등의 실제 물리 컬럼 자체를 DROP해야 한다면, Flyway 마이그레이션 스크립트(`V18__drop_supply_demand_columns.sql` 등)를 추가 작성해야 합니다.
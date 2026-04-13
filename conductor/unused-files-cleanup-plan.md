# 사용되지 않는 파일 및 코드 제거 계획

## Objective
프로젝트 내에서 더 이상 사용되지 않는 도메인 클래스, 사용 중단된(@Deprecated) 메서드, 중복되거나 불필요한 테스트/문서 파일들을 식별하고 제거하여 유지보수성을 향상시킵니다.

## Key Files & Context
조사 결과, 아래 항목들이 제거 대상으로 식별되었습니다:

**1. 도메인 및 유틸리티 클래스:**
- `InvestorSupplyDemand`: 이전 마이그레이션(`V18__drop_supply_demand_columns_from_stock_price.sql`) 및 관련 계획에 의해 `StockPrice` 엔티티에서 완전히 분리되었으나, 도메인 클래스 파일과 일부 서비스의 `import` 구문이 남아있습니다.
- `org.stockwellness.global.common.ApiResponse`: 프로젝트에서는 `org.stockwellness.global.common.response.ApiResponse`를 사용 중이며, 본 파일은 참조되는 곳이 없는 중복된 클래스입니다.

**2. @Deprecated 및 데드 코드:**
- `PortfolioStatBatchService.updatePortfolioStats`: `updatePortfolioStatsBatch`로 대체된 후 사용처가 존재하지 않는 데드 코드입니다.

**3. 불필요한 테스트 파일 및 클래스:**
- `stockwellness-api/.../StockwellnessApplicationTests.java`: 내용이 비어있고 `@Disabled` 처리된 무의미한 테스트입니다.
- `stockwellness-core/.../TestAdapterOut.java`: 아무런 로직이 없고 참조되지 않는 빈 컴포넌트 테스트용 클래스입니다.
- `stockwellness-core/.../TestCoreApplication.java`: 테스트용 `@SpringBootApplication`으로 정의되었으나, 아무 테스트에서도 사용하지 않습니다.

**4. 불필요한 기획/테스트 문서:**
- `conductor/tracks/test/test.txt`: 의미 없는 임시 테스트 파일입니다.
- `conductor/stockprice_supplydemand_removal_plan.md`: 이미 `conductor/archive/.../plan.md`로 아카이빙된 파일과 내용이 동일한 루트 위치의 파일입니다.

## Implementation Steps

**Step 1: 사용되지 않는 Java 클래스 및 Import 제거**
- 삭제 대상: 
    - `stockwellness-core/src/main/java/org/stockwellness/domain/stock/price/InvestorSupplyDemand.java`
    - `stockwellness-core/src/main/java/org/stockwellness/global/common/ApiResponse.java`
- Import 제거 대상:
    - `stockwellness-batch/src/main/java/org/stockwellness/application/StockPriceSyncService.java` (`InvestorSupplyDemand`)
    - `stockwellness-batch/src/main/java/org/stockwellness/batch/job/stockprice/sync/application/StockPriceBatchService.java` (`InvestorSupplyDemand`)
    - `stockwellness-core/src/main/java/org/stockwellness/application/service/portfolio/internal/PortfolioAnalysisDataLoader.java` (`InvestorSupplyDemand`)

**Step 2: 데드 코드(Deprecated 메서드) 제거**
- `stockwellness-core/src/main/java/org/stockwellness/application/service/portfolio/PortfolioStatBatchService.java`에서 `@Deprecated public void updatePortfolioStats(Portfolio portfolio)` 메서드 삭제

**Step 3: 불필요한 테스트 클래스 제거**
- `stockwellness-api/src/test/java/org/stockwellness/StockwellnessApplicationTests.java` 파일 삭제
- `stockwellness-core/src/test/java/org/stockwellness/adapter/out/persistence/TestAdapterOut.java` 파일 삭제
- `stockwellness-core/src/test/java/org/stockwellness/TestCoreApplication.java` 파일 삭제

**Step 4: 불필요한 Conductor 파일 제거**
- `conductor/tracks/test/` 디렉토리 전체 삭제
- `conductor/stockprice_supplydemand_removal_plan.md` 파일 삭제

## Verification & Testing
- 삭제 작업 완료 후 `./gradlew clean build`를 실행하여 컴파일 에러가 발생하지 않는지 확인합니다.
- 전체 테스트 코드가 정상적으로 통과하는지 검증합니다.
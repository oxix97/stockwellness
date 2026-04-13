# Implementation Plan: Sector Code Consolidation

## 1. Objective
현재 `stock` 테이블과 `StockSector` 도메인에 분산되어 있는 `sector_large_code`, `sector_medium_code`, `sector_small_code` 3개의 코드를 단일 `sector_code` 컬럼/필드로 통합하여 데이터 구조를 단순화하고 조회 로직을 개선합니다. 단일 코드는 다음 우선순위에 따라 결정됩니다.
1. `sector_small_code`가 "0000"이 아니고 유효한 경우 사용
2. `sector_medium_code`가 "0000"이 아니고 유효한 경우 사용
3. `sector_large_code`가 "0000"이 아니고 유효한 경우 사용
4. 전부 "0000"이거나 무효한 경우 `sector_large_code`의 값을 기본값(미분류 등)으로 사용

## 2. Key Files & Context

*   **Database Migration**: `stockwellness-core/src/main/resources/db/migration/V20__merge_sector_codes.sql` (신규)
*   **Domain**: `stockwellness-core/src/main/java/org/stockwellness/domain/stock/StockSector.java`
*   **Repository/Adapter**: 
    *   `stockwellness-core/src/main/java/org/stockwellness/adapter/out/persistence/stock/StockAdapter.java`
    *   `stockwellness-core/src/main/java/org/stockwellness/adapter/out/persistence/stock/repository/StockRepository.java`
    *   `stockwellness-core/src/main/java/org/stockwellness/adapter/out/persistence/stock/repository/StockCustomRepositoryImpl.java`
*   **Port**: `stockwellness-core/src/main/java/org/stockwellness/application/port/out/stock/StockPort.java`
*   **Batch Service**: `stockwellness-batch/src/main/java/org/stockwellness/batch/job/sector/SectorEodBatchService.java`
*   **Tests/Fixtures**:
    *   `stockwellness-core/src/test/java/org/stockwellness/domain/stock/StockSectorTest.java`
    *   `stockwellness-core/src/testFixtures/java/org/stockwellness/fixture/StockFixture.java`
    *   `stockwellness-batch/src/test/java/org/stockwellness/batch/job/sector/SectorEodBatchServiceTest.java`
    *   `stockwellness-core/src/test/java/org/stockwellness/application/service/stock/SectorAnalysisServiceTest.java`
    *   `stockwellness-core/src/test/java/org/stockwellness/application/service/portfolio/PortfolioAnalysisDiversificationTest.java`

## 3. Implementation Steps

### Step 1: 데이터베이스 마이그레이션 스크립트 작성 (`V20__merge_sector_codes.sql`)
1.  `stock` 테이블에 `sector_code` 컬럼(VARCHAR(10))을 추가합니다.
2.  기존 3개 컬럼의 값을 바탕으로 새로운 `sector_code`에 값을 마이그레이션(UPDATE) 합니다. 
    *   SQL `CASE WHEN` 구문을 사용하여 `small` > `medium` > `large` 우선순위로 값이 "0000"이 아닌 첫 번째 값을 채택합니다.
3.  기존 `sector_large_code`, `sector_medium_code`, `sector_small_code` 컬럼을 삭제(DROP)합니다.

### Step 2: `StockSector.java` 도메인 모델 리팩토링
1.  기존 `largeCode`, `mediumCode`, `smallCode` 필드를 제거하고, `sectorCode` 필드를 하나만 유지합니다.
2.  팩토리 메서드 `of(large, medium, small, indexMap)`의 시그니처는 외부 인터페이스(마스터 동기화 등) 호환을 위해 유지하되, 내부적으로 우선순위 로직(small -> medium -> large)을 적용해 `sectorCode` 하나를 추출하도록 수정합니다. (단, 업종명 매핑 우선순위는 기존 그대로 소->중->대로 가져옵니다).
3.  그 외 `empty()`, `of(...)` 메서드들을 단일 `sectorCode` 기반으로 생성하도록 수정합니다.

### Step 3: 리포지토리 및 포트 인터페이스 수정
1.  `StockRepository`의 `findBySector_MediumCodeAndStatus` 메서드명을 `findBySector_SectorCodeAndStatus`로 변경합니다.
2.  `StockPort` 및 `StockAdapter`에서 `findBySectorMediumCode` 메서드명을 `findBySectorCode`로 변경하고 구현부를 수정합니다.
3.  `StockCustomRepositoryImpl`에서 3개 필드에 대한 다중 OR 검색 로직을 `sectorCode` 단일 검색으로 간소화합니다.

### Step 4: 배치 로직 및 외부 호출부 수정
1.  `SectorEodBatchService`의 `getMappingCode()` 메서드 등에서 `mediumCode` 및 `largeCode`를 추출하던 중복 로직을 제거하고, 엔티티의 통합된 `sectorCode`를 직접 호출하여 사용하도록 정리합니다.

### Step 5: 테스트 코드 최신화
1.  `StockSectorTest`에서 새로 작성된 통합 로직에 맞춰 팩토리 생성 및 코드/이름 매핑 검증 테스트를 업데이트합니다.
2.  `StockFixture`, `SectorEodBatchServiceTest` 등 각 테스트에서 `StockSector.of`를 호출하는 부분을 새로운 내부 동작에 맞춰 확인하고 컴파일 에러를 해결합니다.

## 4. Verification & Testing
*   Flyway DB 마이그레이션이 기존 데이터를 올바르게 단일 `sector_code`로 변환하는지 로컬 DB 실행 후 확인합니다.
*   `./gradlew clean build` 명령어로 전체 모듈의 컴파일 에러를 해소하고, 관련 단위 테스트가 정상적으로 통과하는지 확인합니다.
*   실제 EOD 배치 및 마스터 동기화 배치가 `sector_code` 하나만으로도 정상 동작하는지 검증합니다.
# Specification: 해외 지수 시세 조회 로직 업데이트

## 1. Overview
KIS(한국투자증권) API의 해외 지수 일자별 차트 조회(`FHKST03030100`) 호출 양식과 응답 데이터 포맷이 변경됨에 따라, 이를 애플리케이션 내의 `KisDailyPriceAdapter` 및 관련 DTO 구조에 반영하여 배치 동기화 작업(`BenchmarkPriceSyncJobConfig`)이 정상적으로 수행되도록 업데이트하는 작업입니다.

## 2. Functional Requirements
*   **API 파라미터 업데이트:** `fetchOverseasIndexDailyPrices` 메서드 호출 시 KIS API 요구사항(`FID_INPUT_DATE_1`="D", `FID_INPUT_DATE_2`="D")에 맞게 요청 파라미터를 수정합니다.
*   **DTO 매핑 수정:**
    *   상세 데이터(`KisOverseasIndexDailyPrice`)의 변경된 필드명(`ovrs_nmix_prpr`, `ovrs_nmix_oprc` 등)을 정확하게 매핑합니다.
    *   요약 데이터(`OverseasIndexSummary`)의 변경된 필드명을 정확하게 매핑합니다.
*   **인터페이스 구현:** `KisOverseasIndexDailyPrice`가 `BenchmarkPriceData` 인터페이스를 구현(implements)하도록 하여, 공통 배치 로직에서 다형성을 활용해 처리할 수 있도록 합니다.
*   **배치 연동 수정:** `BenchmarkPriceSyncJobConfig`에서 해외 지수를 호출할 때 변경된 메서드 시그니처(`endDate` 등 필요 시)와 올바르게 연동되도록 수정합니다.

## 3. Non-Functional Requirements
*   **안정성:** KIS API 호출 실패 시 재시도(`@Retry`) 로직이 정상 작동해야 합니다.
*   **하위 호환성:** 기존 국내 지수 조회 로직(`fetchIndexDailyPrices`)에는 영향을 주지 않아야 합니다.

## 4. Acceptance Criteria
*   해외 지수(S&P 500, NASDAQ 등)에 대한 일별 시세 데이터가 KIS API를 통해 정상적으로 수집되어야 합니다.
*   수집된 데이터가 `BenchmarkPrice` 엔티티로 변환되어 DB에 올바르게 저장되거나 업데이트되어야 합니다.
*   `BenchmarkPriceSyncJobConfig` 배치 작업 실행 시 해외 지수 관련 오류(ClassCastException, NoSuchMethodError 등)가 발생하지 않아야 합니다.

## 5. Out of Scope
*   국내 주식 및 지수 조회 로직 변경
*   배치 스케줄링 주기 자체의 변경
*   프론트엔드 UI 변경
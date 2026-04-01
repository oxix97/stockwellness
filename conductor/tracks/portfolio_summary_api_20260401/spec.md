# Specification: 포트폴리오 분석 요약 API 고도화

## 1. Overview
현재 `GET /api/v1/portfolios/{id}/analysis/summary` API는 가치 평가(Valuation), 분산도(Diversification), 리밸런싱 가이드(Rebalancing)의 기본 정보만 반환하고 있습니다.
본 트랙에서는 사용자가 지정한 기간에 기반하여 심도 있는 성과 분석 지표(CAGR, 변동성, 알파, 수익 기여도)를 추가하고, 내부 로직을 개선하여 완성도 높은 포트폴리오 요약 정보를 제공하는 것을 목표로 합니다.

## 2. Functional Requirements
*   **API 엔드포인트 수정**:
    *   `GET /api/v1/portfolios/{id}/analysis/summary`
    *   **요청 파라미터 추가**: 분석 기준 기간을 설정할 수 있는 사용자 지정 파라미터 (`startDate`, `endDate` 또는 조회 기간 `months`) 추가.
*   **추가 분석 지표 제공**: 응답 DTO에 다음 지표를 계산하여 포함.
    *   **CAGR (연평균 성장률)**
    *   **변동성 (Annualized Volatility)**
    *   **알파 (Alpha)**
    *   **종목별 수익 기여도 (Contribution)**
*   **통계 결측치 처리**:
    *   MDD, Sharpe Ratio, Beta 등의 지표는 기존 방식대로 배치 작업(Batch)을 통해 DB에 저장된 값(`PortfolioStats`)만 사용. 부재 시 기본값 반환(응답 지연 방지).

## 3. 내부 로직 점검 및 수정 사항 (Controller & Service)
*   `PortfolioAnalysisController.java` 점검 및 수정:
    *   `getAnalysisSummary` 메서드에 기간 파라미터(`@RequestParam`) 바인딩 추가.
    *   Service 레이어로 파라미터를 전달하도록 DTO/Command 구조 업데이트.
*   `PortfolioAnalysisService.java` 수정:
    *   사용자 지정 기간에 맞춰 과거 데이터를 조회하고, 새로운 지표(CAGR, 변동성, 알파, 기여도)를 계산하는 로직 (`PortfolioAnalysisUseCase.getAnalysisSummary` 및 관련 반환 레코드) 수정.

## 4. Acceptance Criteria
*   [ ] `GET /api/v1/portfolios/{id}/analysis/summary` API가 기간 파라미터를 정상 처리한다.
*   [ ] 응답에 CAGR, 변동성, 알파, 수익 기여도가 정확히 계산되어 포함된다.
*   [ ] 기존 배치 기반 통계 지표(MDD 등)는 안전하게 처리된다.
*   [ ] 관련 Controller 및 Service 레이어의 테스트가 추가되고 통과한다.
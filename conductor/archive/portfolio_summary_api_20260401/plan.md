# Implementation Plan: 포트폴리오 분석 요약 API 고도화

## Phase 1: 도메인 및 서비스 레이어 지표 계산 구현 (TDD)
- [x] Task: `PortfolioAnalysisServiceTest` 및 지표 계산 유틸리티 테스트 업데이트 (CAGR, 변동성, 알파, 수익 기여도 계산 검증).
- [x] Task: `PortfolioAnalysisUseCase`, `PortfolioAnalysisSummaryResult` 및 관련 Record에 새로운 조회 기간 파라미터(`startDate`, `endDate`) 및 지표 필드 추가.
- [x] Task: `PortfolioAnalysisService.getAnalysisSummary` 로직 구현 - 지정된 기간에 맞춰 `SimulationData`를 로드하고, 내부 계산기(예: `BacktestCalculator` 확장)를 통해 신규 지표 동적 계산. 기존 DB 통계치(MDD 등)는 그대로 사용.
- [x] Task: Conductor - User Manual Verification 'Phase 1: 도메인 및 서비스 레이어 지표 계산 구현 (TDD)' (Protocol in workflow.md)

## Phase 2: 컨트롤러 및 API 응답 DTO 반영 (TDD)
- [x] Task: `PortfolioAnalysisControllerTest` 업데이트 - `getAnalysisSummary` 엔드포인트에 쿼리 파라미터(`startDate`, `endDate`)를 추가하고, 응답 결과에 새로운 지표가 포함되는지 검증 (Spring REST Docs 문서화 포함).
- [x] Task: `PortfolioAnalysisController` 및 `PortfolioFacade` 수정 - 기간 파라미터(`@RequestParam`) 바인딩 및 Service 레이어 호출.
- [x] Task: 응답 DTO(`PortfolioAnalysisSummaryResponse`, `PortfolioValuationResponse` 등)에 신규 지표(CAGR, 변동성, 알파, 기여도) 추가 및 매핑 구현.
- [x] Task: Conductor - User Manual Verification 'Phase 2: 컨트롤러 및 API 응답 DTO 반영 (TDD)' (Protocol in workflow.md)
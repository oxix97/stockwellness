# Specification: Portfolio API Alignment

## Overview
포트폴리오 메인 화면이 사용하는 API 계약을 현재 백엔드 구현 기준으로 고정하고, 프론트엔드가 안전하게 소비할 수 있도록 문서와 테스트를 정리합니다. 이 작업은 신규 기능 추가 없이 기존 구현과 문서의 불일치를 해소하는 것을 목적으로 합니다.

## Functional Requirements
- `PortfolioController`, `PortfolioAnalysisController`의 현재 응답 구조를 기준 계약으로 확정합니다.
- 아래 엔드포인트의 REST Docs/OpenAPI 계약이 실제 응답과 일치하는지 검증합니다.
    - `GET /api/v1/portfolios/{portfolioId}/analysis/summary`
    - `GET /api/v1/portfolios/{portfolioId}`
    - `GET /api/v1/portfolios/{portfolioId}/health`
    - `GET /api/v1/portfolios/{portfolioId}/advice/latest`
    - `POST /api/v1/portfolios/{portfolioId}/advice`
    - `GET /api/v1/portfolios/{portfolioId}/analysis/performance/inception/chart`
    - `GET /api/v1/portfolios/{portfolioId}/analysis/correlation`
- 특정 응답 필드 구조를 확인하고 고정합니다.
    - `summary.diversification.assetRatios`, `sectorRatios`, `countryRatios`: `name`, `value` 구조 유지
    - `summary.rebalancing`: `lastUpdated` 필드가 없음을 명확히 유지
    - `inception/chart`: 비교 지표 데이터가 프론트에서 `ticker`를 key로 사용할 수 있도록 현재 구조 유지
- 루트 문서 `docs/specs/screen-api-mapping/portfolio.md`를 실제 구현 기준으로 **완전히 새로 작성(전체 덮어쓰기)**합니다.
    - 메인 화면은 `summary` 재사용 구조라는 점 명시
    - `analysis/diversification`, `analysis/rebalancing`은 존재하나 메인 플로우에서 직접 호출하지 않는다는 점 반영

## Non-Functional Requirements
- 기존에 프론트엔드에서 기대하던 하위 호환 필드(`lastUpdated`, `category`, `ratio` 등)는 추가하지 않습니다.
- 포트폴리오 메인 화면 기준 신규 엔드포인트는 추가하지 않습니다.

## Acceptance Criteria
- [ ] 명시된 모든 엔드포인트에 대한 REST Docs 테스트가 현재 백엔드 응답을 정확히 반영하여 통과해야 합니다.
- [ ] `docs/specs/screen-api-mapping/portfolio.md` 문서가 실제 API 응답 스키마와 100% 일치하도록 업데이트되어야 합니다.
- [ ] 불필요한 레거시 필드가 응답 DTO나 문서에 포함되지 않았음을 확인해야 합니다.

## Out of Scope
- `PortfolioRebalancingResponse` 확장 (제품 요구로 리밸런싱 산출 시각이 명시적으로 필요해질 때까지 보류)
- `HealthDiagnosis` 전용 집계 API 개발 (프론트 최적화 요구 발생 시 판단)
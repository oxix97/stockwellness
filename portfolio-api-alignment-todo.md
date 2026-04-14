# 포트폴리오 API 정합성 작업 목록

## 목적

포트폴리오 메인 화면이 사용하는 API 계약을 현재 백엔드 구현 기준으로 고정하고, 프론트엔드가 안전하게 소비할 수 있도록 문서와 테스트를 정리한다.

## 백엔드에서 해야 할 일

### 1. API 계약 고정

- `PortfolioController`, `PortfolioAnalysisController`의 현재 응답 구조를 기준 계약으로 확정한다.
- 프론트의 과거 기대값에 맞춘 호환 필드(`lastUpdated`, `category`, `ratio`)는 추가하지 않는다.
- 포트폴리오 메인 화면 기준 신규 엔드포인트는 추가하지 않는다.

### 2. 계약 검증 강화

- 아래 엔드포인트의 REST Docs/OpenAPI 계약이 실제 응답과 일치하는지 점검한다.
- `GET /api/v1/portfolios/{portfolioId}/analysis/summary`
- `GET /api/v1/portfolios/{portfolioId}`
- `GET /api/v1/portfolios/{portfolioId}/health`
- `GET /api/v1/portfolios/{portfolioId}/advice/latest`
- `POST /api/v1/portfolios/{portfolioId}/advice`
- `GET /api/v1/portfolios/{portfolioId}/analysis/performance/inception/chart`
- `GET /api/v1/portfolios/{portfolioId}/analysis/correlation`

### 3. 응답 필드 확인 포인트

- `summary.diversification.assetRatios`, `sectorRatios`, `countryRatios`는 `name`, `value` 구조를 유지한다.
- `summary.rebalancing`에는 현재 `lastUpdated`가 없음을 명확히 유지한다.
- `inception/chart`의 비교 지표 데이터는 프론트가 `ticker`를 key로 사용할 수 있게 현재 구조를 유지한다.

### 4. 문서 정리

- 루트 문서 `docs/specs/screen-api-mapping/portfolio.md`와 실제 구현이 다르면 즉시 수정한다.
- 메인 화면은 `summary` 재사용 구조라는 점을 문서에 유지한다.
- `analysis/diversification`, `analysis/rebalancing`은 존재하지만 현재 메인 플로우에서 직접 호출하지 않는다는 점을 문서에 반영한다.

### 5. 후속 검토 항목

- 제품 요구로 리밸런싱 산출 시각이 꼭 필요해질 때만 `PortfolioRebalancingResponse` 확장을 검토한다.
- `HealthDiagnosis` 전용 집계 API가 필요한지 여부는 프론트 최적화 요구가 생긴 뒤 판단한다.

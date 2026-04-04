# Track: 포트폴리오 건강 진단 내 종목별 기여도 로직 구현

## 🎯 Objective
포트폴리오 건강 진단(`GET /api/v1/portfolios/{id}/health`) 결과에 각 종목이 포트폴리오 성과에 미치는 기여도(`StockContributionResult`)를 실제 데이터 기반으로 계산하여 제공합니다.

## 🏗️ Implementation Details

### 1. 백테스트 결과(BacktestResult) 확장
- `BacktestResult` 레코드에 `Map<String, BigDecimal> itemReturns` 필드 추가.
- `BacktestEngine` (`runLumpSum`, `runDCA`)에서 시뮬레이션 종료 시 각 종목별 누적 수익률(포트폴리오 전체 투자금 대비 손익 비율) 계산 로직 추가.
- `BacktestCalculator`가 성과 지표 계산 시 `itemReturns`를 유지하여 반환하도록 수정.

### 2. 건강 진단 계산(PortfolioHealthCalculator) 강화
- `CalculatedHealth` 레코드에 `List<StockContributionResult> stockContributions` 필드 추가.
- `PortfolioHealthCalculator`에서 `backtestResult.itemReturns()`를 기반으로 종목별 기여도 분석:
    - **주요 수익원**: 수익률 상위 3개 종목 (점수 80~100점).
    - **보조 수익원**: 그 외 수익 발생 종목 (점수 70~90점).
    - **수익성 개선 필요**: 마이너스 수익률 종목 (점수 0~50점).
- 각 상태에 따른 동적 `reason` 텍스트 생성.

### 3. 진단 서비스(PortfolioDiagnosisService) 연동
- `PortfolioDiagnosisService`에서 `CalculatedHealth`의 기여도 리스트를 `PortfolioHealthResult`에 매핑하여 반환.
- 기존의 `// TODO: Stock Contributions` 주석 해결.

## ✅ Verification Results
- `PortfolioHealthCalculator` 단위 테스트: 종목별 수익률에 따른 기여도 분류 및 점수 산출 검증.
- `PortfolioDiagnosisService` 통합 테스트: 최종 API 응답 형태에 기여도 데이터 포함 여부 확인.
- 전체 모듈 빌드 및 테스트 통과 (`./gradlew test`).

## 🔗 Related Files
- `org.stockwellness.application.service.portfolio.internal.BacktestResult`
- `org.stockwellness.application.service.portfolio.internal.BacktestEngine`
- `org.stockwellness.application.service.portfolio.internal.BacktestCalculator`
- `org.stockwellness.application.service.portfolio.internal.CalculatedHealth`
- `org.stockwellness.application.service.portfolio.internal.PortfolioHealthCalculator`
- `org.stockwellness.application.service.portfolio.PortfolioDiagnosisService`

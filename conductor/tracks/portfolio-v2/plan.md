# [전체 포트폴리오 지표 활성화 및 실시간 대응 강화 계획]

## 🎯 목표 (Objective)
- 모든 KOSPI/KOSDAQ 활성 종목에 대해 최소 2년 이상의 시세 및 최신 수급 데이터를 확보합니다.
- 사용자가 포트폴리오의 종목을 추가하거나 변경했을 때, 배치를 기다리지 않고 즉시 지표가 재계산되는 **이벤트 기반 아키텍처**를 구축합니다.
- 데이터가 누락된 경우에도 사용자에게 0이 아닌 유효한 정보를 제공할 수 있도록 **Fallback 로직**을 강화합니다.

---

## 🛠️ 세부 실행 과제 (Tasks)

### Task 1: 전 종목 시세 및 수급 데이터 동기화 (Batch Infrastructure)
- **내용:** `stockPriceBatchJob`을 실행하여 전체 약 2,500개 종목에 대해 과거 2년치 데이터와 기관/외국인 순매수 정보를 DB에 적재합니다.
- **수정 사항:** 
    - `StockPriceProcessor`에서 수급 데이터 수집 범위를 최근 30일로 고정하고, 효율적인 멀티 조회를 수행하도록 최적화.
    - KIS API Rate Limit(초당 20건)을 고려한 안정적인 배치 완주 보장.

### Task 2: 이벤트 기반 실시간 통계 업데이트 (Core Application)
- **내용:** 사용자가 포트폴리오를 생성하거나 수정할 때(`updateItems`), 즉시 통계가 재계산되도록 이벤트를 발행하고 처리합니다.
- **수정 사항:**
    - `PortfolioUpdatedEvent` 정의.
    - `PortfolioCommandService`에서 `ApplicationEventPublisher`를 통해 이벤트 발행.
    - `PortfolioStatEventListener` 구현: `PortfolioStatBatchService.processSinglePortfolio()`를 호출하여 `portfolio_stats` 테이블 즉시 갱신.

### Task 3: 데이터 정합성 보장 및 Fallback 로직 (Core/API)
- **내용:** 신규 종목 추가 시 과거 이력이 없는 경우를 대비하여 우선순위 동기화 및 API 레벨에서의 계산 로직을 보강합니다.
- **수정 사항:**
    - `PortfolioAnalysisDataLoader`: 시세 데이터가 비어 있는 경우, 즉시 KIS API를 통해 실시간 가격을 가져오는 Fallback 로직 추가.
    - `/valuation` API: `PortfolioStats` 정보가 `null`일 경우에도 매수가 기반의 최소한의 지표를 산출하여 반환.

---

## 🚀 단계별 구현 및 테스트 전략 (Testing Strategy)
1. **[Batch]** 전체 동기화 배치를 실행하고 DB의 `stock_price` 건수 확인.
2. **[Core]** 포트폴리오 수정 API 호출 후 `portfolio_stats` 테이블이 즉시 업데이트되는지 확인.
3. **[API]** 새로운 종목 추가 직후 `/valuation`과 `/summary` 호출 시 0이 아닌 유효한 수익률/위험 지표가 반환되는지 검증.

---

## 📝 커밋 메시지 제안 (Git Commit Messages)

1. `feat(batch): support full stock price and supply/demand synchronization`
2. `feat(core): trigger portfolio statistics update on portfolio changes via events`
3. `feat(core): add data-fetching fallback for portfolios with missing stock history`
4. `fix(api): ensure all valuation metrics are computed correctly in summary API`

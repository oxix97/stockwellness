# Backend API Alignment & Support Tasks

프론트엔드 API 클라이언트 보강 시 백엔드에서 지원하거나 검토해야 할 사항들입니다.

## 1. 🔴 API 문서 및 스펙 현행화 (High Priority)

### A. OpenAPI Spec 최신화
프론트엔드가 신규 API(Market, Stock Detail 등)를 호출할 수 있도록 OpenAPI 스펙을 재생성하세요.
- [ ] `./gradlew updateOpenApiSpec` 실행하여 `openapi.yaml` 갱신.
- [ ] 프론트엔드에 최신 스키마 정보 전달.

### B. `StockDetailResult` 필드 검증
프론트엔드가 종목 상세 화면 구현 시 필요한 필수 데이터가 누락되지 않았는지 확인하세요.
- [ ] `StockDetailResult`에 `ticker`, `name`, `sectorName`, `marketType`, `currentPrice`, `fluctuationRate`, `isMarketOpen` 등 포함 여부 확인.

### C. `MarketIndexResult` 필드 검증
- [ ] `MarketIndexResult`에 지수명(KOSPI 등), 현재가, 등락률, 등락금액이 올바르게 포함되는지 확인.

---

## 2. 🟡 데이터 및 로직 지원 (Medium Priority)

### A. 포트폴리오 분석 데이터 정합성
- [ ] `DiagnosisResponse` 및 `PortfolioAnalysisSummaryResponse`의 데이터 구조가 프론트엔드 UI(Recharts 등)에서 사용하기 편리한지 검토.

### B. 섹터 비교 데이터 정합성
- [ ] `compareWithMarket` 결과값에 시장 지수와 섹터의 추이를 동시에 그릴 수 있도록 충분한 기간의 데이터가 포함되는지 확인.

---

## 3. 🟢 기타 사항
- [ ] 신규 추가될 프론트엔드 API 테스트에 대응하여 필요시 테스트용 Mock 데이터 보강.
- [ ] 프론트엔드에서 리밸런싱 주기(`rebalancingPeriod`) 등을 Enum 타입으로 엄격하게 관리할 수 있도록 응답 값 형식을 상수로 명확히 정의.

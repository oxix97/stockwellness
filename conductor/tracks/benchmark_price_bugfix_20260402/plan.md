# Track: 국내 시장 지수 수익률 0% 노출 버그 수정

## 🎯 Objective
- `/api/v1/market/indexes` 호출 시 KOSPI, KOSDAQ 등 국내 시장 지수의 등락률(수익률)이 0%로 고정 노출되는 현상을 해결합니다.

## 🐛 Root Cause
- KIS API에서 국내 지수 일별 시세 조회 시 전일 대비율(`bstp_nmix_prdy_ctrt`)이 0 또는 null로 내려오는 경우가 빈번함.
- `BenchmarkPriceDataProcessor`가 해외 지수에 대해서만 수동 계산을 수행하고, 국내 지수는 API 응답 값을 그대로 신뢰하여 저장하기 때문.

## 🏗️ Remediation Plan
### 1. `BenchmarkPriceDataProcessor.java` 로직 수정
- **변경 사항**: 조건부 수동 계산 로직 도입.
- 기존에 `type.isOverseas()`인 경우에만 수동으로 전일 종가 기반 수익률을 계산하던 로직을 확장.
- 국내 지수라 할지라도 KIS API가 반환한 `changeRate`가 `null`이거나 `0`인 경우, 시스템에 저장된 직전 거래일의 종가(`prevClose`)를 조회(또는 파라미터로 확보)하여 수동으로 등락률을 계산하도록 보완.

### 2. 검증 (Validation)
- `BenchmarkPriceDataProcessorTest` 단위 테스트 작성/수정:
    - 외부 API가 등락률 0을 반환했을 때 수동 로직이 개입하여 정상적인 등락률을 반환하는지 검증.
- 로컬 DB 적용 후 `api` 모듈 테스트로 최종 응답 검증.

## 🔗 Related Files
- `stockwellness-batch/src/main/java/org/stockwellness/batch/job/stock/price/BenchmarkPriceDataProcessor.java`
- `stockwellness-batch/src/test/java/org/stockwellness/batch/job/stock/price/BenchmarkPriceDataProcessorTest.java`
- `feedback.md` (참고 문서)

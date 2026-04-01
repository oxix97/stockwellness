# Track: 수급 상위 섹터 데이터 중복 오류 수정

## 🎯 Objective
- `/api/v1/sectors/ranking/supply` 호출 시 모든 섹터의 외인/기관 순매수 금액이 KOSPI 종합 시장 전체의 금액으로 동일하게 반환되는 치명적인 버그를 수정합니다.

## 🐛 Root Cause
- KIS API 호출 어댑터(`KisSectorAdapter.java`)의 `fetchInvestorTradingDaily` 메서드 내부에서 API 파라미터로 `FID_INPUT_ISCD_1`("KSP"), `FID_INPUT_ISCD_2`("0001")가 하드코딩되어 있습니다.
- "0001"은 코스피 종합 지수를 의미하며, API는 `FID_INPUT_ISCD`(요청한 섹터 코드)를 무시하고 KOSPI 전체의 투자자 매매동향을 응답으로 내렸습니다. 

## 🏗️ Remediation Plan
### 1. `KisSectorAdapter.java` 로직 수정
- **변경 사항**: `fetchInvestorTradingDaily` 메서드 내 URI Builder의 `FID_INPUT_ISCD_1`, `FID_INPUT_ISCD_2` 쿼리 파라미터를 빈 문자열(`""`)로 수정하거나 제거하여 섹터(`U` 시장 구분)별 정확한 수급 데이터가 조회되도록 강제합니다.

### 2. 검증 (Validation)
- 로컬 또는 단위 테스트를 실행하여 특정 섹터 코드를 넘겼을 때 응답으로 나오는 매매동향 데이터가 전체 시장 데이터(-7조원 대)가 아닌 정상 범위의 개별 데이터인지 확인합니다.

## 🔗 Related Files
- `stockwellness-core/src/main/java/org/stockwellness/adapter/out/external/kis/adapter/KisSectorAdapter.java`

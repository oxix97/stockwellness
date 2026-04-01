# [조사 보고서 및 수정 계획서] 국내 시장 지수 수익률 0% 노출 문제

## 1. 현상 요약
- **문제**: `/api/v1/market/indexes` 호출 시 코스피(KOSPI), 코스닥(KOSDAQ) 등 국내 지수의 수익률(등락률)이 `0%`로 고정되어 노출됨.
- **영향**: 서비스 홈 화면 및 시장 지수 컴포넌트에서 정확한 시장 변동 정보를 제공하지 못함.

---

## 2. 원인 분석 결과
### 2.1 데이터 수집 단계 (Batch)
- 국내 지수 데이터는 `BenchmarkPriceSyncJob` 배치를 통해 수집되며, 내부적으로 `BenchmarkPriceDataProcessor`가 데이터를 가공합니다.
- **로직 분석**:
    ```java
    // BenchmarkPriceDataProcessor.java
    BigDecimal changeRate = detail.prdyCtrt(); // API 제공값 (기본 대비율)

    if (type.isOverseas()) {
        // 해외 지수만 전일 종가 기반으로 직접 계산 수행
        // ... 수동 계산 로직 ...
    }
    ```
- 현재 로직은 **해외 지수(`isOverseas == true`)만 직접 계산**하도록 설계되어 있으며, 국내 지수는 한국투자증권(KIS) API가 제공하는 값을 그대로 사용합니다.

### 2.2 외부 API 응답 특성
- 국내 업종/지수 기간별 시세 API(`FHKUP03500100`)의 일별 차트 데이터 응답에서 전일 대비율(`bstp_nmix_prdy_ctrt`) 필드가 데이터가 비어 있거나 `0`으로 반환되는 경우가 빈번하게 발생합니다.
- 배치는 이 `0` 값을 그대로 DB에 저장하므로, 최종 API 응답에서도 0%로 노출되는 것입니다.

---

## 3. 수정 계획 (Remediation Plan)

### 3.1 수정 대상
- **파일**: `stockwellness-batch/src/main/java/org/stockwellness/batch/job/stock/price/BenchmarkPriceDataProcessor.java`

### 3.2 수정 전략: "조건부 수동 계산 도입"
국내 지수라 하더라도 API가 제공하는 등락률 정보가 없거나 0인 경우, 시스템 내부의 전일 종가 데이터를 활용하여 직접 등락률을 산출하도록 로직을 확장합니다.

**변경 계획 코드:**
```java
// 2. 해외 지수이거나, 등락률 데이터가 0(또는 null)인 국내 지수인 경우 수동 계산 수행
if (type.isOverseas() || (changeRate == null || changeRate.compareTo(BigDecimal.ZERO) == 0)) {
    // 2-1. 전일 종가(T-1) 확보 (메모리 맵 또는 DB 조회)
    if (prevClose == null) {
        prevClose = benchmarkPricePort.findLatestBefore(ticker, detail.baseDate())
                .map(BenchmarkPrice::getClosePrice)
                .orElse(null);
    }

    // 2-2. 전일 종가가 존재할 경우 현재가와 비교하여 등락률(changeRate) 직접 계산
    if (prevClose != null && prevClose.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal diff = close.subtract(prevClose);
        changeRate = diff.divide(prevClose, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}
```

---

## 4. 기대 효과 및 검증
- **효과**: 외부 API 데이터 누락과 상관없이 항상 정확한 시장 지수 수익률을 산출하여 사용자에게 신뢰성 있는 정보를 제공함.
- **검증**:
    1. `BenchmarkPriceDataProcessor` 단위 테스트를 통해 0% 응답 시 계산 로직 작동 여부 확인.
    2. 로컬 배치 실행 후 `benchmark_price` 테이블의 `change_rate` 데이터 업데이트 확인.
    3. `/api/v1/market/indexes` API의 실제 응답 필드(`fluctuationRate`) 검증.

---
**작성일**: 2026-04-02
**작성자**: Gemini CLI (Senior Software Engineer)

# Track stockprice_supply_bugfix_20260411 Context

- [Specification](./spec.md)
- [Metadata](./metadata.json)

## 구현 기록

- 수급 데이터 저장 수정은 `8bcd1788`에서 구현·테스트됐고 현재 `develop`에 포함돼 있다.
- 후속 `30019bcf`와 V18 migration에서 수급 데이터를 `StockPrice` 임베디드 컬럼에서 분리했다. 현재 데이터 검증 대상은 `stock_investor_trade`다.
- `spec.md`의 원본 KIS 응답과 실제 적재값 비교 인수는 여전히 유효하다. 새 테이블 기준의 수동 운영 검증 증거는 확인되지 않았다.
- 완료된 절차 계획은 정리 작업에서 제거됐으며 복구 사본과 체크섬은 `/private/tmp/stockwellness-document-cleanup-20261002/manifest.json`에 기록했다.

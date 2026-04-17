# Code Review — Sector Cache Failure Recovery & Robustness Update

## 요약
캐시 키 생성 실패(`Null key`) 및 Redis 역직렬화 오류(`SerializationException`) 대응을 위한 캐시 전략 표준화 및 버전 관리 적용.

## 심각도 기준
- 🔴 BLOCKER: 머지 전 반드시 수정
- 🟡 MAJOR: 강력 권고 수정
- 🟢 MINOR: 선택적 개선

## 리뷰 항목

### 🔴 BLOCKER
- ~~**캐시 무효화 불일치 (StockPriceUpdateConsumer)**~~: (해결됨) `StockPriceUpdateConsumer.java`의 캐시 무효화 로직이 업데이트된 버전(`:v3`, `:v2`)을 사용하도록 수정되었습니다.

### 🟡 MAJOR
- **캐시 키 표준화**: `@Cacheable`의 `key`를 `#p0`와 같은 인덱스 기반으로 변경하여 런타임 시 파라미터 이름 유실로 인한 `IllegalArgumentException`을 원천 차단함.
- **입력값 검증**: `SectorInsightService`의 주요 진입점에서 `sectorCode`에 대한 null/blank 체크를 추가하여 `ErrorCode.INVALID_INPUT_VALUE`를 반환하도록 개선됨. (API 경계 검증 준수)
- **컴파일러 설정**: `build.gradle.kts`에 `-parameters` 옵션을 추가하여 SpEL에서 명시적 이름을 사용할 때의 안정성을 높임. (다만 현재 코드는 인덱스 기반 `#p0`를 사용하여 이중 안전장치 확보)

### 🟢 MINOR
- **캐시 설정 누락**: `StockPriceUpdateConsumer`에서 사용하는 `portfolio_valuation`, `portfolio_diversification`, `portfolio_rebalancing` 캐시는 현재 `CacheType`에 등록되어 있지 않아 기본 설정(1시간 TTL, 도메인 직렬화기)을 따르게 됩니다. 이들도 `CacheType`에 통합하여 관리하는 것을 권장합니다.
- **캐시 버전 관리**: `CacheType` 접미사를 통한 버전 관리 전략은 구 데이터와의 충돌을 방지하는 아주 좋은 방식입니다.

## 결론
**APPROVE**
모든 결함이 수정되었으며, 테스트를 통해 정상 동작이 확인되었습니다.

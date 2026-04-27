# Spec: TechnicalScoreService 지표 확장 및 신규 정책 도입

이 문서는 `TechnicalScoreService`에서 사용하지 않던 기술적 지표(ADX, MACD)를 점수 산출 로직에 통합하고, 이에 대한 단위 테스트를 보강하기 위한 설계를 정의합니다.

## 1. 개요
- **대상 클래스**: 
    - `org.stockwellness.domain.stock.analysis.TechnicalScoreService` (데이터 모델 확장)
    - `org.stockwellness.domain.stock.analysis.BasicScoringPolicies` (신규 정책 구현)
- **핵심 목표**: 
    - `IndicatorSnapshot`에 ADX, MACD 관련 필드 추가
    - 추세 강도를 반영하는 `AdxPolicy` 구현
    - 추세 전환을 포착하는 `MacdPolicy` 구현
    - 신규 정책 및 기존 정책의 경계값 단위 테스트 강화
- **테스트 도구**: JUnit 5, AssertJ, Mockito

## 2. 데이터 모델 확장 (`IndicatorSnapshot`)

`TechnicalIndicators` 엔티티로부터 더 풍부한 데이터를 가져오도록 `IndicatorSnapshot` 레코드를 확장합니다.

| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| `plusDi` | `BigDecimal` | ADX 보조 지표 (상승 강도) |
| `minusDi` | `BigDecimal` | ADX 보조 지표 (하락 강도) |
| `macd` | `BigDecimal` | MACD 지수 |
| `macdSignal` | `BigDecimal` | MACD 시그널 |
| `isMacdCross` | `boolean` | MACD 골든/데드크로스 여부 |

## 3. 신규 정책 로직 설계 (Approach A)

### 3.1. AdxPolicy (추세 신뢰도 강화)
ADX가 일정 수치(25) 이상일 때 현재 진행 중인 배열 상태(`alignment`)의 점수를 강화합니다.
- **가점 (+5)**: `adx > 25` 이고 `alignment == PERFECT` (정배열)
- **감점 (-5)**: `adx > 25` 이고 `alignment == REVERSE` (역배열)
- **유지 (0)**: 그 외 모든 상황

### 3.2. MacdPolicy (추세 전환 포착)
MACD와 Signal의 크로스 여부와 상대적 위치를 통해 변곡점을 점수화합니다.
- **가점 (+10)**: `isMacdCross == true` 이고 `macd > macdSignal` (골든크로스)
- **감점 (-10)**: `isMacdCross == true` 이고 `macd < macdSignal` (데드크로스)
- **유지 (0)**: 그 외 모든 상황

## 4. 테스트 시나리오 설계

### 4.1. 정책별 단위 테스트 (`BasicScoringPoliciesTest`)
각 정책 클래스를 독립적으로 테스트하여 배점 로직의 정확성을 검증합니다.
- **RsiPolicy**: 30.0 미만, 정확히 30.0, 70.0 초과, 정확히 70.0일 때의 점수 확인.
- **AdxPolicy**: ADX 25 기준점 상하에서의 점수 변화 확인.
- **MacdPolicy**: 크로스 플래그가 `false`일 때 값이 교차되어도 점수가 0점인지 확인.

### 4.2. 서비스 통합 테스트 (`TechnicalScoreServiceTest`)
- **Confluence 시나리오**: 모든 지표가 매수 신호일 때(RSI < 30 + 골든크로스 + ADX 강화 + MACD 골든크로스) 최종 점수가 100점으로 클램핑되는지 확인.
- **Null Safety**: 모든 추가된 필드가 `null`로 들어와도 예외 없이 50점을 유지하는지 확인.

## 5. 완료 조건 (Success Criteria)
- [ ] `IndicatorSnapshot` 필드 확장 및 `from()` 매핑 로직 업데이트 완료.
- [ ] `AdxPolicy`, `MacdPolicy` 클래스 구현 및 `TechnicalScoreService`에 등록 완료.
- [ ] JUnit 5 `@Nested` 구조를 적용하여 정책별/서비스별 테스트 코드 보강 완료.
- [ ] 전체 빌드 및 테스트 통과 확인 (`./gradlew :stockwellness-core:test`).

# Spec: PortfolioFacade 단위 테스트 보강

이 문서는 `PortfolioFacade` 클래스의 신뢰성을 확보하기 위한 단위 테스트 보강 설계를 정의합니다. 얇은 위임 계층(Thin Layer)으로서의 역할과 내부적인 파라미터 보정 로직을 철저히 검증하는 것을 목표로 합니다.

## 1. 개요
- **대상 클래스**: `org.stockwellness.application.service.portfolio.PortfolioFacade`
- **핵심 목표**: 
    - `getAnalysisSummary` 메서드의 날짜 보정 로직 전수 검증
    - `aiEnabled` 플래그에 따른 AI 기능(진단/조언)의 일관된 분기 검증
    - UseCase의 예외가 Facade를 통해 호출자에게 정확히 전달되는지 검증
- **테스트 도구**: JUnit 5, AssertJ, Mockito

## 2. 테스트 구조 설계
가독성과 유지보수성을 위해 JUnit 5의 `@Nested` 구조를 채택합니다.

```java
@ExtendWith(MockitoExtension.class)
class PortfolioFacadeTest {
    // 공통 Mock 및 필드 정의

    @Nested
    @DisplayName("포트폴리오 분석 및 요약 테스트")
    class AnalysisTests {
        // getAnalysisSummary 관련 테스트
    }

    @Nested
    @DisplayName("AI 기능 토글 테스트")
    class AiToggleTests {
        // aiEnabled 기반 분기 테스트
    }

    @Nested
    @DisplayName("예외 전파 테스트")
    class ExceptionTests {
        // 예외 위임 테스트
    }
}
```

## 3. 세부 테스트 시나리오

### 3.1. 분석 요약 날짜 보정 (`getAnalysisSummary`)
- **Case 1: `startDate`, `endDate` 모두 Null**
    - `endDate` == 오늘
    - `startDate` == 오늘 - 12개월
- **Case 2: `startDate`는 있고 `endDate`가 Null**
    - `endDate` == 오늘
    - `startDate` == 사용자 입력값 유지
- **Case 3: `endDate`는 있고 `startDate`가 Null**
    - `endDate` == 사용자 입력값 유지
    - `startDate` == 입력된 `endDate` - 12개월

### 3.2. AI 기능 일관성 (`aiEnabled`)
- **Case 1: AI 비활성화 (`aiEnabled = false`)**
    - `diagnosePortfolio`, `getLatestAdvice`, `getNewAdvice` 호출 시 `mock()` 데이터가 반환되는지 확인.
    - 실제 UseCase(`diagnosePortfolioUseCase`, `aiAdvisorUseCase`)와의 상호작용이 전혀 없음을 검증 (`verifyNoInteractions`).
- **Case 2: AI 활성화 (`aiEnabled = true`)**
    - 각 메서드가 실제 UseCase로 파라미터를 정확히 전달하며 호출되는지 확인.

### 3.3. 예외 전파 (Exception Handling)
- **UseCase 예외 위임**
    - `managePortfolioUseCase.createPortfolio()` 호출 시 `RuntimeException` 발생 시뮬레이션.
    - `portfolioFacade.createPortfolio()` 호출 시 동일한 예외가 발생하는지 검증.

## 4. 완료 조건 (Success Criteria)
- [ ] `@Nested` 구조를 적용하여 테스트 코드 재구성 완료.
- [ ] 날짜 보정 로직의 3가지 조합에 대해 각각 독립적인 테스트 케이스가 존재하고 통과함.
- [ ] AI 비활성화 시 UseCase 호출 차단 여부가 `verifyNoInteractions`로 증명됨.
- [ ] 모든 테스트 실행 시 100% 성공 확인 (`./gradlew :stockwellness-core:test`).

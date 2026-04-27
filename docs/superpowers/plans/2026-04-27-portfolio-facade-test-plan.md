# PortfolioFacade 단위 테스트 보강 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `PortfolioFacade`의 구조적 테스트 재구성 및 날짜 보정, AI 토글, 예외 전파 시나리오 완벽 검증.

**Architecture:** JUnit 5 `@Nested`를 사용하여 관심사별로 테스트를 그룹화하고, Mockito를 이용해 UseCase 위임 및 필드 주입(`aiEnabled`) 상황을 시뮬레이션합니다.

**Tech Stack:** Java 21, Spring Boot 3.4, JUnit 5, AssertJ, Mockito

---

### Task 1: 환경 격리 및 준비

**Files:**
- Create: `.worktrees/task-233/` (Branch: `task/#233-portfolio-facade-test`)

- [ ] **Step 1: 새로운 Git Worktree 생성**
Run: `git worktree add .worktrees/task-233 -b task/#233-portfolio-facade-test develop` (명령은 `stockwellness` 루트에서 실행)

- [ ] **Step 2: 작업 디렉토리 이동 및 확인**
Run: `cd .worktrees/task-233 && ls`

### Task 2: 테스트 구조 리팩토링 (@Nested 도입)

**Files:**
- Modify: `stockwellness-core/src/test/java/org/stockwellness/application/service/portfolio/PortfolioFacadeTest.java`

- [ ] **Step 1: 기존 테스트 코드를 @Nested 구조로 재구성**
기존의 평면적인 테스트들을 `@Nested` 클래스 안으로 옮깁니다.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioFacade 단위 테스트")
class PortfolioFacadeTest {
    @InjectMocks private PortfolioFacade portfolioFacade;
    @Mock private ManagePortfolioUseCase managePortfolioUseCase;
    @Mock private LoadPortfolioUseCase loadPortfolioUseCase;
    @Mock private PortfolioAnalysisUseCase portfolioAnalysisUseCase;
    @Mock private DiagnosePortfolioUseCase diagnosePortfolioUseCase;
    @Mock private AiAdvisorUseCase aiAdvisorUseCase;

    private static final Long MEMBER_ID = 1L;
    private static final Long PORTFOLIO_ID = 100L;

    @Nested
    @DisplayName("기본 위임 테스트")
    class DelegationTests {
        @Test
        @DisplayName("관리/조회/분석 요청이 각 UseCase로 전달되는지 확인")
        void delegateBasicUseCases() {
            // 기존 delegateManageUseCase, delegateLoadUseCase, delegateAnalysisUseCase 로직 통합 또는 분리 수록
        }
    }
}
```

- [ ] **Step 2: 테스트 실행 및 통과 확인**
Run: `./gradlew :stockwellness-core:test --tests "org.stockwellness.application.service.portfolio.PortfolioFacadeTest"`

### Task 3: 날짜 보정 로직 세부 검증 (getAnalysisSummary)

**Files:**
- Modify: `stockwellness-core/src/test/java/org/stockwellness/application/service/portfolio/PortfolioFacadeTest.java`

- [ ] **Step 1: Case 1 - startDate만 있는 경우 테스트 추가**
```java
@Test
@DisplayName("시작일만 있고 종료일이 없으면 종료일은 오늘로 보정한다")
void getAnalysisSummary_OnlyStartDate() {
    LocalDate startDate = LocalDate.of(2023, 1, 1);
    portfolioFacade.getAnalysisSummary(MEMBER_ID, PORTFOLIO_ID, startDate, null);

    ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
    verify(portfolioAnalysisUseCase).getAnalysisSummary(eq(MEMBER_ID), eq(PORTFOLIO_ID), eq(startDate), endCaptor.capture());
    assertThat(endCaptor.getValue()).isEqualTo(LocalDate.now());
}
```

- [ ] **Step 2: Case 2 - endDate만 있는 경우 테스트 추가**
```java
@Test
@DisplayName("종료일만 있고 시작일이 없으면 시작일은 종료일 기준 12개월 전으로 보정한다")
void getAnalysisSummary_OnlyEndDate() {
    LocalDate endDate = LocalDate.of(2024, 12, 31);
    portfolioFacade.getAnalysisSummary(MEMBER_ID, PORTFOLIO_ID, null, endDate);

    verify(portfolioAnalysisUseCase).getAnalysisSummary(MEMBER_ID, PORTFOLIO_ID, endDate.minusMonths(12), endDate);
}
```

- [ ] **Step 3: 테스트 실행 및 통과 확인**
Run: `./gradlew :stockwellness-core:test`

### Task 4: AI 기능 토글 일관성 검증

**Files:**
- Modify: `stockwellness-core/src/test/java/org/stockwellness/application/service/portfolio/PortfolioFacadeTest.java`

- [ ] **Step 1: AI 비활성화 시 모든 AI 관련 메서드가 UseCase와 상호작용하지 않는지 확인**
```java
@Test
@DisplayName("AI 비활성화 시 진단/최신조언/신규조언 모두 UseCase를 호출하지 않는다")
void aiDisabled_NoInteractions() {
    ReflectionTestUtils.setField(portfolioFacade, "aiEnabled", false);
    
    portfolioFacade.diagnosePortfolio(MEMBER_ID, PORTFOLIO_ID);
    portfolioFacade.getLatestAdvice(MEMBER_ID, PORTFOLIO_ID);
    portfolioFacade.getNewAdvice(MEMBER_ID, PORTFOLIO_ID);

    verifyNoInteractions(diagnosePortfolioUseCase, aiAdvisorUseCase);
}
```

- [ ] **Step 2: 테스트 실행 및 통과 확인**

### Task 5: 예외 전파 검증

**Files:**
- Modify: `stockwellness-core/src/test/java/org/stockwellness/application/service/portfolio/PortfolioFacadeTest.java`

- [ ] **Step 1: UseCase 예외가 그대로 던져지는지 확인하는 테스트 추가**
```java
@Test
@DisplayName("UseCase에서 예외 발생 시 Facade는 이를 그대로 전파한다")
void shouldPropagateException() {
    given(managePortfolioUseCase.createPortfolio(any())).willThrow(new RuntimeException("Domain Error"));

    assertThatThrownBy(() -> portfolioFacade.createPortfolio(mock(CreatePortfolioCommand.class)))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Domain Error");
}
```

### Task 6: 최종 확인 및 정리

- [ ] **Step 1: 전체 테스트 및 빌드 확인**
Run: `./gradlew clean build`

- [ ] **Step 2: 커밋 및 PR 생성**
```bash
git add .
git commit -m "test(core): PortfolioFacade 테스트 구조 리팩토링 및 시나리오 보강 (#233)"
gh pr create --base develop --title "test(core): PortfolioFacade 단위 테스트 보완 (#233)" --body "Closes #233"
```

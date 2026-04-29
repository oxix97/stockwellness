# TechnicalScoreService 지표 확장 및 신규 정책 도입 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 미사용 중인 기술적 지표(ADX, MACD)를 점수 산출 로직에 통합하고, 신규 정책 구현 및 단위 테스트 강화를 통해 기술적 분석 점수의 정밀도를 높임.

**Architecture:** `IndicatorSnapshot` 레코드를 확장하고, 전략 패턴 기반의 `ScoringPolicy`를 추가 구현하여 `TechnicalScoreService`에 주입합니다. JUnit 5 `@Nested`를 사용하여 정책별 독립 검증을 수행합니다.

**Tech Stack:** Java 21, Spring Boot 3.4, JUnit 5, AssertJ

---

### Task 1: 환경 격리 및 준비

**Files:**
- Create: `.worktrees/task-234/` (Branch: `task/#234-technical-score-test`)

- [ ] **Step 1: 새로운 Git Worktree 생성**
Run: `git worktree add .worktrees/task-234 -b task/#234-technical-score-test develop` (명령은 `stockwellness` 루트에서 실행)

- [ ] **Step 2: 작업 디렉토리 이동 및 확인**
Run: `cd .worktrees/task-234 && ls`

### Task 2: 데이터 모델 확장 (IndicatorSnapshot)

**Files:**
- Modify: `stockwellness-core/src/main/java/org/stockwellness/domain/stock/analysis/TechnicalScoreService.java`

- [ ] **Step 1: IndicatorSnapshot 레코드에 필드 추가 및 from() 매핑 업데이트**
```java
public record IndicatorSnapshot(
        AlignmentStatus alignment,
        BigDecimal rsi,
        BigDecimal adx,
        BigDecimal plusDi,      // 추가
        BigDecimal minusDi,     // 추가
        BigDecimal macd,        // 추가
        BigDecimal macdSignal,  // 추가
        boolean isGoldenCross,
        boolean isDeadCross,
        boolean isMacdCross,    // 추가
        BigDecimal closePrice,
        BigDecimal bbLower,
        BigDecimal bbUpper
) {
    public static IndicatorSnapshot from(TechnicalIndicators ti, BigDecimal closePrice) {
        return new IndicatorSnapshot(
                ti.getAlignmentStatus(),
                ti.getRsi14(),
                ti.getAdx(),
                ti.getPlusDi(),
                ti.getMinusDi(),
                ti.getMacd(),
                ti.getMacdSignal(),
                Boolean.TRUE.equals(ti.getIsGoldenCross()),
                Boolean.TRUE.equals(ti.getIsDeadCross()),
                Boolean.TRUE.equals(ti.getIsMacdCross()),
                closePrice,
                ti.getBollingerLower(),
                ti.getBollingerUpper()
        );
    }
}
```

- [ ] **Step 2: 컴파일 확인**
Run: `./gradlew :stockwellness-core:classes`

### Task 3: 신규 정책 구현 (AdxPolicy, MacdPolicy)

**Files:**
- Modify: `stockwellness-core/src/main/java/org/stockwellness/domain/stock/analysis/BasicScoringPolicies.java`

- [ ] **Step 1: AdxPolicy 구현 (추세 강화)**
```java
@Component
public static class AdxPolicy implements ScoringPolicy {
    @Override
    public int evaluate(TechnicalScoreService.IndicatorSnapshot snapshot) {
        if (snapshot.adx() == null || snapshot.adx().compareTo(BigDecimal.valueOf(25)) <= 0) {
            return 0;
        }
        return switch (snapshot.alignment()) {
            case PERFECT -> 5;
            case REVERSE -> -5;
            default -> 0;
        };
    }
}
```

- [ ] **Step 2: MacdPolicy 구현 (추세 전환)**
```java
@Component
public static class MacdPolicy implements ScoringPolicy {
    @Override
    public int evaluate(TechnicalScoreService.IndicatorSnapshot snapshot) {
        if (!snapshot.isMacdCross() || snapshot.macd() == null || snapshot.macdSignal() == null) {
            return 0;
        }
        return snapshot.macd().compareTo(snapshot.macdSignal()) > 0 ? 10 : -10;
    }
}
```

### Task 4: 테스트 구조 리팩토링 및 보강 (@Nested)

**Files:**
- Create: `stockwellness-core/src/test/java/org/stockwellness/domain/stock/analysis/BasicScoringPoliciesTest.java` (신규 정책 전용 테스트)
- Modify: `stockwellness-core/src/test/java/org/stockwellness/domain/stock/analysis/TechnicalScoreServiceTest.java`

- [ ] **Step 1: 개별 정책 단위 테스트 작성 (BasicScoringPoliciesTest)**
각 정책의 경계값(RSI 30/70, ADX 25, MACD 크로스)을 독립적으로 검증합니다.

- [ ] **Step 2: TechnicalScoreServiceTest 리팩토링**
`@Nested` 구조를 도입하고 신규 정책이 포함된 통합 점수(Confluence) 시나리오를 추가합니다.

- [ ] **Step 3: 전체 테스트 실행 및 통과 확인**
Run: `./gradlew :stockwellness-core:test`

### Task 5: 최종 확인 및 정리

- [ ] **Step 1: 전체 빌드 및 회귀 테스트 확인**
Run: `./gradlew clean build`

- [ ] **Step 2: 커밋 및 PR 생성**
```bash
git add .
git commit -m "feat(core): 기술적 분석 지표 확장 및 신규 점수 정책 도입 (#234)"
gh pr create --base develop --title "feat(core): 기술적 분석 점수 정밀화 및 테스트 보강 (#234)" --body "Closes #234"
```

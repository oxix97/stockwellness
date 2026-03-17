# Gradle Build Warnings Resolution Plan

## Objective
Gradle 빌드 리포트(`problems-report.html`)에서 발견된 경고(Warning) 및 권고(Advice) 사항들을 해결하여 빌드 안정성과 향후 도구 호환성(Gradle 10 등)을 확보합니다.

## Priorities & Solutions

### 우선순위 1. [안정성] Unchecked/Unsafe Operations 해결
*   **대상 파일**: `stockwellness-batch/src/main/java/org/stockwellness/batch/job/stock/sector/job/step/SectorInsightItemWriter.java`
*   **문제점**: 제네릭 타입 캐스팅 시 `(List<SectorInsight>) items`와 같이 사용하여 Unchecked 경고 발생. 런타임 타입 안정성에 잠재적 위험 존재.
*   **해결 방안**: 
    *   타입 안정성을 보장하도록 `sectorInsightPort.saveAll`의 시그니처를 `List<? extends SectorInsight>`로 유연하게 수용하도록 변경하거나, Java 21의 타입 추론에 맞게 컬렉션 처리를 개선합니다.

### 우선순위 2. [미래 호환성] Gradle `Task.project` Deprecation 해결
*   **대상 모듈**: `stockwellness-api` (`build.gradle.kts` 내 `openapi3` 태스크)
*   **문제점**: 태스크 실행 시점에 `Task.project` 속성을 호출하는 방식이 사용됨. 이는 Gradle Configuration Cache와 호환되지 않으며, **Gradle 10에서는 에러로 처리**될 예정입니다.
*   **해결 방안**:
    *   `build.gradle.kts` 파일의 최상단에 정의된 `com.epages.restdocs-api-spec` 플러그인(현재 `0.19.4`)을 Configuration Cache를 지원하는 최신 버전으로 업데이트를 검토합니다.
    *   `build.gradle.kts` 내 `afterEvaluate` 블록 대신 Gradle의 지연 속성(Lazy Properties / Task Configuration Avoidance)을 사용하도록 스크립트를 리팩토링합니다.

### 우선순위 3. [코드 품질] Deprecated API 교체
*   **대상 파일**: `stockwellness-core/src/main/java/org/stockwellness/application/service/portfolio/internal/AdvisorAiDataLoader.java`
*   **문제점**: 코드 내에서 사용이 중단된(Deprecated) API를 호출하고 있습니다.
*   **해결 방안**:
    *   컴파일 시 `-Xlint:deprecation` 옵션을 적용해 원인 메서드를 정확히 식별합니다. (예: 생성자 호출, 라이브러리 메서드 등)
    *   식별된 API를 최신 권장 API로 안전하게 교체합니다.

## Implementation Steps
1. **타입 경고 제거**: `SectorInsightItemWriter` 및 관련 Port의 제네릭 타입을 수정합니다.
2. **빌드 스크립트 수정**: OpenAPI 관련 태스크 의존성을 지연 초기화 방식으로 개선하고 플러그인 버전을 점검합니다.
3. **Deprecated 수정**: `AdvisorAiDataLoader` 내 코드를 수정합니다.
4. **검증**: 전체 수정 완료 후 `./gradlew clean build`를 실행하여 3가지 유형의 문제가 리포트에서 완전히 사라졌는지 확인합니다.

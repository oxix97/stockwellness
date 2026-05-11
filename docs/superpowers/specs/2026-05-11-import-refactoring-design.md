# Design Specification: Import Optimization & FQCN Refactoring

## 1. 개요 (Overview)
`stockwellness` 프로젝트 내의 불필요한 import를 제거하고, 코드 내에 직접 명시된 전체 클래스 경로(FQCN)를 상단 import로 추출하여 단순 클래스명으로 치환한다. 이는 프로젝트 코딩 표준(`GEMINI.md`)을 준수하고 가독성을 향상시키는 것을 목적으로 한다.

## 2. 문제 정의 (Problem Statement)
- **미사용 Import**: 105건 발견. 코드의 복잡도를 높이고 유지보수를 방해함.
- **FQCN 직접 사용**: 175건 발견. `GEMINI.md`의 "Never use FQCN inline. Always import at the top." 규칙을 위반함.

## 3. 목표 (Goals)
- 모든 자바 소스 파일에서 미사용 import 제거.
- 코드 내 FQCN 사용 사례를 `import`와 `SimpleName` 사용으로 자동 전환.
- 리팩토링 후 모든 모듈의 컴파일 성공 보장.

## 4. 상세 설계 (Detailed Design)

### 4.1. 리팩토링 자동화 도구 설계
Python 스크립트를 사용하여 다음 로직을 순차적으로 수행한다:
1. **분석 단계**: 각 파일에서 FQCN 패턴을 추출하고, 기존 import 리스트와 병합.
2. **FQCN 치환**: 코드 본문에서 FQCN을 Simple Name으로 치환.
3. **Import 재구성**: 
    - 새로운 FQCN에 대한 import 추가.
    - 미사용 import 제거.
    - Import 섹션 정렬 (알파벳순, `java.*` 우선 등).
4. **부작용 방지**: 치환 전후의 클래스명 충돌 여부 체크.

### 4.2. 작업 순서
1. **stockwellness-core**: 가장 많은 비즈니스 로직과 도메인이 포함된 핵심 모듈 먼저 진행.
2. **stockwellness-batch**: 배치 모듈 처리.
3. **stockwellness-api**: API 모듈 처리.

## 5. 검증 계획 (Verification Plan)
- **컴파일 체크**: `core`, `batch`, `api` 각 모듈에 대해 `./gradlew compileJava compileTestJava` 실행.
- **정적 분석**: 리팩토링 후 분석 스크립트를 재실행하여 Unused Import 및 FQCN Usage가 0인지 확인.
- **동작 확인**: 기존 단위 테스트 및 통합 테스트(`test`) 전체 실행.

## 6. 예외 및 제약 사항
- 동일한 Simple Name을 가진 클래스들이 한 파일에서 사용될 경우(예: `java.util.Date`와 `java.sql.Date`), 한 쪽은 FQCN을 유지해야 함. 이 경우 작업을 중단하고 보고함.
- Wildcard import(`*`)는 명시적 import로 풀지 않고 그대로 유지함 (현행 유지).

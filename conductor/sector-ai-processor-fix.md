# SectorAiItemProcessor Test Failure Fix Plan

## Objective
`stockwellness-batch` 모듈의 테스트 실패 원인인 `SectorAiItemProcessor`의 미구현된 비즈니스 로직을 완성하여 빌드를 정상화합니다.

## Root Cause
`SectorAiItemProcessor.java`의 `process` 메서드 내부가 주석(`// ... (중략) ...`)으로만 되어 있어, 실제 AI 분석 요청 및 결과 업데이트 로직이 누락되어 있습니다. 이로 인해 테스트 케이스에서 기대하는 `AiOpinion` 업데이트가 발생하지 않아 `AssertionError`가 발생했습니다.

## Proposed Solution
`SectorAiItemProcessor.java`의 `process` 메서드에 실제 분석 로직을 구현합니다.

### Changes
1.  **`SectorAiItemProcessor.java` 수정**:
    -   `SectorAiContext` 객체 생성 로직 추가.
    -   `loadSectorAiPort.generateSectorOpinion(context)` 호출을 통해 AI 분석 결과(`AiReport`) 수령.
    -   `insight.updateAiOpinion()`을 호출하여 수령한 분석 결과를 엔티티에 반영.
    -   발생 가능한 예외 상황에 대한 로깅 및 Fallback 처리 보완.

## Implementation Steps
1.  `stockwellness-batch/src/main/java/org/stockwellness/batch/job/stock/sector/job/step/SectorAiItemProcessor.java` 파일의 `process` 메서드를 구현으로 채웁니다.
2.  해당 테스트(`SectorAiItemProcessorTest`)를 단독 실행하여 성공 여부를 확인합니다.
3.  프로젝트 전체 빌드(`./gradlew clean build`)를 실행하여 최종 검증합니다.

## Verification
-   테스트 케이스 2건 모두 성공 확인:
    -   `AI 분석 결과가 SectorInsight에 임베디드 타입으로 업데이트되는지 확인` -> **PASS**
    -   `기술적 지표에 따른 TrendStatus(정배열) 판별 검증` -> **PASS**

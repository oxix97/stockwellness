# 📊 백테스트 API 고도화 및 검증 보완 계획 (Issue #151)

## 1. 목적
- FE와의 백엔드 협의 사항(2026-03-23)을 반영하여 백테스트 기능을 완결성 있게 고도화.
- API 문서화(RestDocs)의 명확성 확보 및 데이터 정합성 보장.
- 실제 서비스 흐름을 검증하는 통합 테스트 시나리오 구축.
- AI 조언 로직의 정교화를 통한 서비스 가치 증대.

## 2. 주요 작업 내용

### 2.1. API 문서화 및 단위 테스트 보완 (`stockwellness-api`)
- **파일**: `PortfolioAnalysisControllerTest.java`
- **수정 사항**:
  - `run_backtest` 테스트의 요청 JSON 필드를 실제 `BacktestRequest` 규격(`strategy`, `amount`, `benchmarkTicker`, `rebalancingPeriod`, `weights`)에 맞게 수정.
  - `strategy` 값을 유효한 값(`LUMP_SUM`, `DCA`)으로 변경.
  - RestDocs `requestFields` 스니펫을 추가하여 FE가 참고할 수 있는 요청 명세서 완성.

### 2.2. 통합 테스트(Integration Test) 시나리오 추가 (`stockwellness-api`)
- **파일**: `PortfolioIntegrationTest.java`
- **수정 사항**:
  - `runBacktest_Success` 테스트 케이스 추가.
  - 포트폴리오 생성 후, 해당 포트폴리오 ID로 백테스트 API(`POST /backtest`)를 호출하여 결과가 반환되는지 확인.

### 2.3. AI 조언 생성 로직(AiAdvisorService) 정교화 (`stockwellness-core`)
- **파일**: `AiAdvisorService.java`
- **수정 사항**:
  - `generateBacktestAdvice` 메서드 개선.
  - `Alpha`, `Beta`, `Volatility` 지표를 활용한 동적 분석 코멘트 생성 로직 추가.

## 3. 검증 계획
- `./gradlew :stockwellness-api:test` 및 `./gradlew :stockwellness-core:test` 실행.
- `build/generated-snippets/portfolio-analysis-backtest` 내 스니펫 파일들의 정합성 확인.
- 최종 빌드 성공 여부 확인.

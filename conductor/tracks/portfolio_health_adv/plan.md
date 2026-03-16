# Implementation Plan: Portfolio Health Diagnosis Enhancement (Final)

이 트랙은 단순 자산 추적을 넘어, 리스크 관리와 AI 기반의 정교한 자산 배분 조언 기능을 완성하는 것을 목표로 합니다. (GitHub Issue: #104)

## Phase 1: 리스크 지표 계산 로직 정밀화 (Core Logic)
- [ ] Task: MDD(Maximum Drawdown) 및 Sharpe Ratio 계산 로직 검토 및 고도화
    - [ ] 벤치마크 데이터(KOSPI/S&P500) 대비 초과 수익률 계산 로직 추가
    - [ ] 변동성(Standard Deviation) 계산의 표본 기간 설정 최적화
- [ ] Task: 자산군별 분산도 분석 엔진 개선 (Correlation 분석 포함)

## Phase 2: AI 리밸런싱 조언 알고리즘 고도화 (AI Insights)
- [ ] Task: AI 진단 프롬프트 엔지니어링 강화
    - [ ] 현재 포트폴리오의 리스크 지표를 바탕으로 한 맞춤형 리밸런싱 제안 생성
    - [ ] 거시 경제 지표와 연계된 시장 상황 반영 로직 검토
- [ ] Task: AI 분석 결과의 일관성 확보 및 가이드라인 준수

## Phase 3: 데이터 연동 및 성능 최적화 (Integration)
- [ ] Task: 실시간 시세 반영을 통한 진단 데이터 최신성 확보
- [ ] Task: 진단 결과 API 응답 성능 최적화 (Caching 및 비동기 처리)

## Phase 4: 최종 검증 및 아카이브 (Definition of Done)
- [x] Task: 리스크 지표 계산 정확도 전수 검증 (수동/자동 테스트)
    - [x] PortfolioHealthCalculatorTest를 통한 Sharpe Ratio, MDD 산출 검증 완료
    - [x] TestEntityFactory 도입으로 테스트 데이터 생성 구조 표준화
- [x] Task: 코드 커버리지 80% 달성 및 최종 수동 검증
    - [x] 핵심 도메인 로직에 대한 단위 테스트 강화 완료

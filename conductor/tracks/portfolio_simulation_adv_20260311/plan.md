# Implementation Plan: Portfolio Simulation & Management (Phase 2)

이 계획은 리밸런싱 계산기, 백테스팅 엔진, 그리고 통계적 리스크 지표 산출 로직 구현을 목표로 합니다.

## Phase 1: 리밸런싱 계산기 및 목표 비중 관리
현재 비중과 목표 비중의 괴리를 계산하고 실질적인 매매 가이드를 제공합니다.

- [x] **Task: 목표 비중 도메인 로직 추가**
    - [x] `PortfolioItem` 또는 별도 테이블에 `targetWeight` 필드 추가
    - [x] 포트폴리오 수준의 목표 비중 합계 검증 로직 (100% 체크)
- [x] **Task: 리밸런싱 엔진 구현**
    - [x] 현재 시장 가치 기반 실시간 비중 계산 로직 고도화
    - [x] 목표 비중 도달을 위한 필요 매매 수량(Buy/Sell Quantity) 계산 유틸리티 구현
- [ ] **Task: 리밸런싱 API 개발**
    - [ ] `GET /api/v1/portfolios/{id}/analysis/rebalancing` 엔드포인트 추가
    - [ ] 현재 수량, 목표 수량, 차이 및 예상 매매 금액 응답 DTO 구현
- [ ] **Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)**

## Phase 2: 백테스팅 엔진 (DCA & 거치식)
과거 시세 데이터를 활용하여 가상의 투자 성과를 시뮬레이션합니다.

- [ ] **Task: 시뮬레이션 데이터 프로바이더 구현**
    - [ ] 최근 2년치 종목별/지수별 EOD 시세 벌크 로딩 기능 (Redis/DB 최적화)
    - [ ] 국내 지수(KOSPI/KOSDAQ) 벤치마크 데이터 연동
- [ ] **Task: 수익률 시뮬레이션 엔진 개발**
    - [ ] 정액 적립식(DCA) 시뮬레이션 로직 (매월 특정일 자동 매수 가정)
    - [ ] 일시불 거치식 시뮬레이션 로직
    - [ ] 배당금 재투자 및 세금 미적용한 단순 수익률 기반 계산
- [ ] **Task: 백테스팅 API 개발**
    - [ ] `POST /api/v1/portfolios/{id}/analysis/backtest` 엔드포인트 추가
    - [ ] 시계열 수익률 데이터 및 벤치마크 대비 성과 응답 구현
- [ ] **Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)**

## Phase 3: 통계적 리스크 지표 및 배치 자동화
MDD, 샤프 지수, 베타 등 복잡한 통계 지표를 정기적으로 산출합니다.

- [ ] **Task: 통계 계산 유틸리티 구현**
    - [ ] MDD(최대 낙폭) 계산 로직 구현
    - [ ] 수익률 표준편차 기반 변동성 및 샤프 지수 계산 로직 구현
    - [ ] 시장 지수 대비 공분산을 활용한 베타(Beta) 계산 로직 구현
- [ ] **Task: 리스크 지표 산출 배치 작업**
    - [ ] 매일 EOD 시세 업데이트 후 전체 포트폴리오 리스크 지표를 갱신하는 Spring Batch Job 구현
    - [ ] 계산된 리스크 지표를 저장할 영속성 레이어(`PortfolioStats`) 추가
- [ ] **Task: 리스크 지표 조회 API**
    - [ ] 포트폴리오 상세 및 분석 응답에 리스크 지표 필드 추가
- [ ] **Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)**

## Phase 4: 상관관계 분석 및 최종 통합
종목 간의 상관관계를 분석하여 분산 투자 효과를 시각화합니다.

- [ ] **Task: 상관계수 행렬 엔진 구현**
    - [ ] Pearson 상관계수 기반 종목 간 가격 변동 유사도 계산 로직
    - [ ] 히트맵(Heatmap) 구성을 위한 행렬 데이터 변환
- [ ] **Task: 상관관계 분석 API**
    - [ ] `GET /api/v1/portfolios/{id}/analysis/correlation` 엔드포인트 추가
- [ ] **Task: 최종 검증 및 문서화**
    - [ ] Phase 2 전체 기능에 대한 통합 테스트 및 API 문서(RestDocs) 갱신
- [ ] **Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)**

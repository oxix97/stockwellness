# 🍏 StockWellness Product Guide

## 1. Product Vision
개인 투자자가 감정에 휘둘리지 않고 **데이터 기반의 객체관적인 의사결정**을 내릴 수 있도록 돕는 지능형 자산 배분 플랫폼입니다. 복잡한 시장 데이터를 AI가 해석하여 사용자 맞춤형 포트폴리오 건강 진단과 전략적 조언을 제공합니다.

## 2. Target Users
- **Individual Investors (Novice)**: 어려운 투자 지표 대신 직관적인 레이더 차트와 AI 조언으로 포트폴리오를 관리하려는 입문자.
- **Experienced Investors (Quants)**: 백테스트 데이터와 기술적 지표(RSI, MACD)를 바탕으로 정밀한 분석을 원하는 숙련된 투자자.
- **Asset Allocators**: 장기적인 관점에서 자산군별 비중 조정을 통한 리밸런싱 전략이 필요한 자산 배분 투자자.

## 3. Core Values
- **Data-Driven Decisions**: 과거 백테스트 결과를 토대로 감정적 매매를 배제한 투자 신뢰성 확보.
- **AI-Powered Insights**: OpenAI GPT-4o-mini 기반의 정교한 분석으로 포트폴리오의 약점과 기회를 포착.
- **Risk-Adjusted Performance**: 단순 수익률이 아닌 MDD, Sharpe ratio 등 위험 대비 성과 지표를 중심으로 한 건강 진단.

## 4. Key Features (MVP)
- **Portfolio Health Diagnosis**: 수익성, 안전성, 분산, 민첩성, 현금 비중 5대 차원 기반 레이더 차트 및 진단.
- **AI Rebalancing Advisor**: 현재 포트폴리오 상태에 따른 최적의 리밸런싱 가이드 및 조언 생성.
- **Sector & Market Insights**: 섹터별 수급 분석 및 기술적 지표를 통한 시장 주도 섹터 탐색.
- **Batch Monitoring & Automation**: 주가 및 지표 계산 배치의 결과를 Kafka 이벤트로 발행하여 시스템 가시성을 확보하고 실패 시 자동 알림을 제공합니다.
- **Advanced Backtesting (Phase 2)**: 거치식/적립식(DCA) 등 다양한 전략 기반의 역사적 수익률 및 성과 시 뮬레이션.
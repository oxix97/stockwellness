# Product Guidelines: Stockwellness

## 1. Design & UX Principles
*   **명확성과 직관성 (Clarity & Intuitiveness):** 금융 데이터는 복잡하므로, 사용자가 한눈에 포트폴리오의 상태와 투자 성과를 파악할 수 있도록 대시보드와 차트를 최대한 직관적으로 설계합니다.
*   **신뢰감 부여 (Trust & Professionalism):** 색상(예: 안정감을 주는 블루/그린 톤 중심), 타이포그래피, 레이아웃을 통해 금융 서비스로서의 전문성과 신뢰감을 사용자에게 전달합니다.
*   **액션 중심 (Action-Oriented):** 데이터 분석 결과(예: AI 리밸런싱 조언, 건전성 경고)를 제공할 때, 사용자가 어떤 행동을 취해야 하는지 명확한 가이드(Call to Action)를 함께 제시합니다.
*   **반응형 디자인 (Responsive Design):** 모바일, 태블릿, 데스크탑 환경 모두에서 금융 지표와 차트가 깨짐 없이 원활하게 제공되도록 반응형 웹 표준을 준수합니다.

## 2. Tone & Voice
*   **전문적이고 객관적인 톤 (Professional & Objective):** 감정을 배제하고 데이터를 기반으로 객관적인 사실과 분석 결과를 전달합니다. 과장된 표현이나 확정적인 수익 보장 문구는 엄격히 금지합니다.
*   **쉽고 친절한 설명 (Accessible & Helpful):** MDD, Sharpe Ratio, Beta 등 어려운 금융 전문 용어에는 툴팁이나 도움말을 통해 초보자도 쉽게 이해할 수 있는 설명을 제공합니다.
*   **사용자 존중 (Respectful):** 지시적인 어조보다는 제안하고 권유하는 부드러운 어조를 사용합니다. (예: "~을 하세요" 보다 "~을 고려해 보시는 것을 추천합니다")

## 3. Engineering & Development Guidelines
*   **안정성 및 무중단 (Stability & Zero-Downtime):** 투자 정보 서비스의 특성상 서비스 중단은 치명적이므로, Blue-Green 배포 등을 통해 높은 가용성을 유지합니다.
*   **빠른 응답 속도 (Performance):** 다량의 시세 데이터 조회 및 백테스트 시뮬레이션 시 병목이 발생하지 않도록 적극적인 캐싱(Redis)과 비동기 처리(Kafka)를 활용합니다.
*   **데이터 정합성 보장 (Data Integrity):** 이벤트 처리 실패 시 Transactional Outbox Pattern 등을 통해 데이터 유실을 방지하고 시스템 간 정합성을 철저히 보장합니다.
*   **보안 중심 (Security First):** 사용자 정보 및 포트폴리오 데이터는 민감 정보로 취급하며, 철저한 인증/인가(OAuth2, JWT)와 시큐어 코딩 원칙을 준수합니다.
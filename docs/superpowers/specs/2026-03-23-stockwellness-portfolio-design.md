# StockWellness: 핀테크 맞춤형 포트폴리오 및 이력서 설계 문서 (Design Spec)

- **작성일**: 2026-03-23
- **프로젝트명**: StockWellness
- **목표**: 핀테크/금융권 백엔드 개발자 지원 (신입/주니어 레벨의 탄탄한 기본기와 아키텍처 설계 및 성능 최적화 경험 강조)

## 1. 개요 및 비전 (Vision)
본 문서는 StockWellness 프로젝트의 핵심 기술적 성과를 **'엔지니어링 챌린지'** 중심으로 재구성하여, 금융권 채용 담당자(현업 엔지니어)가 지원자의 **기술적 깊이, 문제 해결 능력, 그리고 데이터 무결성에 대한 집요함**을 한눈에 파악할 수 있도록 설계된 포트폴리오 가이드라인이다. 특히 백엔드의 고성능 엔진과 프론트엔드의 직관적인 시각화가 결합된 **'완결성 있는 제품 개발 역량'**을 강조한다.

## 2. 핵심 기술 내러티브 (Key Technical Narratives)

### [Challenge 1] Java 21 Virtual Threads 기반의 고성능 시뮬레이션 엔진 & UI
*   **Problem**: 수천 개의 종목 시세를 외부 API(KIS)로 호출하고 복잡한 수익률을 계산하는 과정에서 기존 Platform Thread 모델은 확장성에 한계가 있었으며, 사용자에게 이 복잡한 시뮬레이션 결과를 지연 없이 전달해야 하는 과제가 있었음.
*   **Action**: 
    - **Back-end**: Java 21 **Virtual Threads** 도입으로 I/O 차단 구간의 리소스 점유율 최적화 및 처리량 극대화.
    - **Front-end**: React & Vite 기반의 고성능 UI 구축. 대량의 시뮬레이션 결과 데이터를 효율적으로 렌더링하고, 사용자가 직접 자산 배분 비중을 조절하며 백테스트 결과를 즉각 확인할 수 있는 인터랙티브 UI 구현.
*   **Result**: 초고속 시뮬레이션과 실시간 피드백이 결합된 **'인터랙티브 자산 배분 시뮬레이터'** 완성.

### [Challenge 2] 멀티 모듈 헥사고날 아키텍처를 통한 도메인 무결성 및 품질 확보
*   **Problem**: 금융 로직의 정합성 검증을 위해 기술적 상세 구현에 의존하지 않는 순수한 테스트 환경이 필요했으며, 프론트엔드 역시 복잡한 사용자 흐름에 대한 안정성 보장이 필수적이었음.
*   **Action**: 
    - **Back-end**: **프래그마틱 헥사고날 아키텍처** 도입으로 도메인 계층 격리 및 순수 유닛 테스트(JUnit) 수행.
    - **Front-end**: **Playwright(E2E)**와 **Vitest(Unit)**를 활용한 테스트 자동화 도입으로 프론트엔드 신뢰성 확보.
*   **Result**: 백엔드와 프론트엔드 모두에서 **'테스트 주도 개발(TDD)'** 문화를 정착시켜, 금융 데이터의 정확성과 서비스 안정성을 동시에 확보.

### [Challenge 3] Spring Batch & Redis를 활용한 시세 대시보드 최적화
*   **Problem**: 대량의 종가 데이터 및 기술적 지표(RSI, MACD)를 실시간으로 시각화할 때 발생하는 지연 시간 최소화 필요.
*   **Action**: 
    - **Back-end**: **Spring Batch**를 통한 지표 사전 계산(Pre-calculation) 및 **Redis 계층형 캐싱** 적용.
    - **Front-end**: MUI(Material UI)와 차트 라이브러리를 활용하여, Redis에서 고속으로 서빙되는 지표 데이터를 직관적인 대시보드 형태로 시각화.
*   **Result**: 복잡한 퀀트 지표를 실시간 조회 성능으로 서빙하는 **'전문가급 기술적 분석 대시보드'** 구현.

## 3. 포트폴리오 구성 상세 (Structure)

### 📄 Notion 이력서 (Resume)
1.  **Summary**: "백엔드의 고성능 엔진부터 프론트엔드의 데이터 시각화까지, 품질 중심의 풀스택 개발자"
2.  **Key Experience (StockWellness)**:
    - 3대 기술 챌린지 (Problem-Action-Result 구조) 요약 (BE + FE 통합 성과)
    - DevOps & QA: GitHub Actions, Playwright, Vitest, n8n 자동화
3.  **Skills**: 
    - BE: Java 21, Spring Boot, Kafka, Redis, PostgreSQL
    - FE: React, TypeScript, Vite, MUI, Radix UI

### 🏠 GitHub README (Project Deep Dive)
1.  **Visual Showcase**: 대시보드(B) 및 백테스트(C) 화면 스크린샷과 함께 각 화면이 백엔드의 어떤 기술(Redis, Virtual Threads)과 연결되는지 설명.
2.  **Architecture Diagram**: 백엔드(Hexagonal) + 프론트엔드(React) + 인프라(Docker/Kafka)를 포함한 통합 시스템 아키텍처.
3.  **Deep Dive 섹션**: 위 챌린지에 대한 기술적 상세 설명 및 코드 스니펫.

## 4. 기대 효과 (Success Criteria)
- 핀테크 실무에서 가장 중요하게 여기는 **데이터 무결성, 성능 최적화, 테스트 가능성**을 프로젝트 전반에 걸쳐 증명.
- 신입/주니어임에도 불구하고 **현대적인 백엔드 기술 스택과 아키텍처 설계 능력**을 갖춘 인재임을 강조하여 합격 가능성 제고.

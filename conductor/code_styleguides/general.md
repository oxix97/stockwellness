
# Code Style & Architecture Guide

이 문서는 stockwellness 프로젝트의 모든 백엔드(Java/Spring Boot) 및 인프라 코드에 적용되는 핵심 원칙입니다. 우리는 도메인 로직의 순수성을 지키고, 시스템의 예측 가능성과 유지보수성을 극대화합니다.

## 1. 가독성 및 모던 자바 활용 (Readability & Modern Java)
* **의도를 드러내는 코드:** 코드는 기계보다 사람이 읽기 쉬워야 합니다. 지나치게 복잡하거나 트릭을 쓴 코드는 지양합니다.
* **Java 21 기능의 적극 도입:**
    * 불변(Immutable) 데이터 전달 객체(DTO, Event Payload 등)에는 반드시 **`Record`**를 사용합니다.
    * 도메인 상태(예: 시장 상태 `MarketStatus`, 주문 타입 등)나 제한된 다형성이 필요한 경우 **`Sealed Class`**와 **`Pattern Matching (switch)`**을 사용하여 분기문의 누락을 컴파일 타임에 방지합니다.
* **Virtual Thread 최적화:** I/O 바운드 작업(DB 조회, 외부 API 호출)이 많으므로 Virtual Thread를 적극 활용하되, ThreadLocal 남용을 방지하여 가독성과 성능을 동시에 잡습니다.

## 2. 일관성과 아키텍처 (Consistency & Architecture)
* **헥사고날 아키텍처 (Ports and Adapters):**
    * 코드베이스 전반에 걸쳐 패키지 구조의 일관성을 엄격히 유지합니다. (예: `domain`, `application/port`, `adapter/in`, `adapter/out`).
    * 어떤 경우에도 도메인 계층이 인프라 계층(Spring Data JPA 어노테이션, Kafka 프레임워크 등)을 의존해서는 안 됩니다.
* **네이밍 컨벤션:**
    * 인터페이스(Port)는 행위를 명확히 기술하며(`LoadMarketDataPort`), 구현체(Adapter)는 사용 기술을 명시합니다(`KisMarketDataAdapter`).

## 3. 예외 처리 및 에러 응답 (Exception Handling) - **[Strict Rule]**
* **Raw Exception 사용 금지:** `throw new RuntimeException()`, `throw new Exception()`, `throw new IllegalArgumentException()`과 같은 표준/원시 예외의 직접 발생을 엄격히 금지합니다.
* **GlobalException 및 ErrorCode 필수 사용:**
    * 모든 비즈니스 로직 및 시스템 예외는 프로젝트 표준 예외인 **`GlobalException`**(혹은 이를 상속받은 도메인별 Custom Exception)을 통해서만 던져야 합니다.
    * 예외를 발생시킬 때는 반드시 사전 정의된 `ErrorCode` (Enum)를 주입해야 합니다.
    * 컨트롤러 단에서는 Spring의 `@RestControllerAdvice`를 통해 이를 낚아채어 일관된 JSON 스펙(ErrorResponse)으로 클라이언트에게 반환해야 합니다.
* **아키텍처적 이유 (Why):**
    * 프론트엔드 및 AI 예측 모듈이 API 호출 실패 시 정확한 원인을 식별하고 후속 처리(재시도 로직 등)를 할 수 있도록 보장합니다.
    * 내부 스택 트레이스(Stack Trace)나 인프라스트럭처의 에러 정보(DB 쿼리 에러 등)가 외부로 누출되는 보안 리스크를 원천 차단합니다.

## 4. 코드 포맷팅 및 Import 컨벤션 (Formatting & Imports)
* **Import 구문 분리 및 정렬 강제:** 모든 Java 클래스의 `import` 구문은 성격에 따라 빈 줄(Blank Line)로 그룹화하여 정렬해야 합니다.
* **권장 Import 정렬 순서:**
    1. **Static Imports:** (예: `org.junit.jupiter.api.Assertions.*`)
    2. **Java / Jakarta 표준 라이브러리:** (`java.*`, `jakarta.*`)
    3. **Spring Framework / 외부 라이브러리:** (`org.springframework.*`, `org.apache.kafka.*` 등)
    4. **Stockwellness 내부 도메인 및 패키지:** (`com.stockwellness.*`)
* **Wildcard(`*`) Import 금지:** 클래스 의존성을 명확히 파악하기 위해 와일드카드 임포트는 금지합니다. (단, 테스트 코드의 static assert 메서드 제외)
* **아키텍처적 이유 (Why):**
    * CI/CD 파이프라인 및 다수의 개발자가 협업하는 GitOps 환경에서, 무분별한 Import 순서로 인해 발생하는 불필요한 **Merge Conflict를 방지**합니다.
    * PR(Pull Request) 리뷰 시 해당 클래스가 어떤 외부 모듈에 의존하고 있는지 명확하게 가시화합니다.

## 5. 단순성과 도메인 주도 설계 (Simplicity & DDD)
* **도메인 객체의 자율성:** 비즈니스 로직은 서비스(Service) 클래스가 아닌 **도메인 엔티티(Entity)** 내부에 캡슐화되어야 합니다. 서비스는 파사드(Facade) 역할만 수행합니다.
* **데이터 소스의 단순화:** 복잡한 실시간 데이터 처리는 배제하고, **EOD(종가) 데이터** 기준의 명확한 파이프라인만 구축합니다. 배치 완료 시 Kafka 이벤트(`market-data-updated`)를 발행하여 각 모듈의 결합도를 낮춥니다.

## 6. 문서화 (Documentation)
* **'What'이 아닌 'Why'를 기록:** 코드가 무엇을 하는지는 네이밍으로 설명합니다. 주석에는 "왜 이 방식을 선택했는지", "비즈니스적 배경은 무엇인지"를 반드시 한국어로 작성합니다.
* **퀀트/도메인 지식 문서화:** 기술적 지표(RSI, MACD 등) 계산 로직 등 도메인 지식이 포함된 코드에는 참조한 비즈니스 룰을 Javadoc으로 상세히 명시합니다.
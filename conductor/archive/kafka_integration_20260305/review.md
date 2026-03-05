# 📋 stockwellness-batch 코드 리뷰 리포트

## 1. 아키텍처 및 모듈 구조 요약
프로젝트는 **Job 중심의 패키지 구조**를 채택하여 각 배치 작업(Master Sync, Price Sync, Sector Insight)이 독립적으로 관리되고 있습니다. 또한 `stockwellness-core` 모듈과의 의존성을 `Port/Adapter` 패턴으로 풀어내어 비즈니스 로직과 인프라 로직을 명확히 분리한 점이 인상적입니다.

### 핵심 구성 요소
*   **Controller (`BatchAdminController`)**: 외부(Admin) 요청을 비동기(`CompletableFuture`)로 수신하여 배치를 트리거합니다.
*   **Scheduler (`StockwellnessScheduler`)**: 비즈니스 워크플로우에 따른 **순차적 배치 실행(Chaining)**을 담당하며, 데이터 정합성을 보장합니다.
*   **Infrastructure (`event`, `common`)**: Kafka 이벤트 발행 및 공통 Writer 등 모듈 전반에서 재사용 가능한 유틸리티를 제공합니다.

---

## 2. 주요 분석 및 코드 리뷰

### ✅ 긍정적인 부분
1.  **조건부 이벤트 발행 설계**: `JobParameters`를 활용해 정기 배치와 수동 배치를 구분하고, Kafka 이벤트 발행 여부를 제어한 점은 불필요한 시스템 부하를 줄이는 훌륭한 설계입니다.
2.  **안정적인 예외 처리**: `faultTolerant()`, `retry()`, `skip()` 설정을 통해 외부 API(KIS) 장애나 일시적인 DB 오류에도 배치가 중단되지 않고 완주할 수 있도록 구성되었습니다. 특히 AI 분석(`SectorAiItemProcessor`)에서의 **Fallback 로직** 적용이 돋보입니다.
3.  **성능 최적화**: 
    *   `JdbcBatchItemWriter`를 사용한 벌크 Insert/Update.
    *   `SectorInsightItemProcessor`에서 지연 로딩 방지를 위해 로컬 캐시(Map)를 활용한 데이터 프리페칭(Pre-fetching).
    *   `RateLimiter`를 통한 외부 API 호출 속도 조절.

### ⚠️ 개선 권장 사항 (Technical Debt)

#### 1. 로그 표준화 및 추적성 (Observability)
*   **현황**: 각 리스너와 프로세서마다 로그 형식이 조금씩 다릅니다.
*   **제언**: `MDC(Mapped Diagnostic Context)`를 활용하여 로그에 `JobInstanceId`나 `runId`를 포함시키면, 여러 배치가 동시에 돌 때 특정 작업의 로그만 필터링하기 훨씬 수월해집니다.

#### 2. 비동기 작업 결과 피드백 (`BatchAdminController`)
*   **현황**: `CompletableFuture.runAsync`로 배치를 실행한 뒤 즉시 성공 메시지를 반환합니다. 사용자는 로그를 보기 전까지 실제 성공 여부를 알 수 없습니다.
*   **제언**: 배치 상태 조회 API(`/status/{jobName}`)가 이미 존재하므로, 응답 메시지에 해당 상태를 확인할 수 있는 URL이나 `ExecutionId`를 명확히 반환하는 것이 좋습니다.

#### 3. 대량 데이터 처리 시의 메모리 관리
*   **현황**: `SectorInsightItemProcessor`에서 모든 종목 시세와 섹터 맵을 메모리에 올리고 있습니다.
*   **제언**: 현재 규모에서는 문제없으나, 향후 종목 수가 비약적으로 늘어날 경우 `StepScope` 빈의 생명주기를 고려하여 메모리 사용량을 모니터링해야 합니다.

---

## 3. 세부 영역별 평가

| 영역 | 평가 | 내용 |
| :--- | :---: | :--- |
| **로그(Logging)** | ⭐⭐⭐⭐ | 작업 단계별 로그가 상세함. `StockPriceProgressListener`의 주기적 로그가 유용함. |
| **예외 처리** | ⭐⭐⭐⭐⭐ | 커스텀 예외(`BatchException`) 사용 및 배치 특화 장애 복구 전략이 매우 뛰어남. |
| **확장성** | ⭐⭐⭐⭐ | 새로운 Job 추가 시 패키지 단위로 독립적인 확장이 용이함. |
| **테스트 가능성** | ⭐⭐⭐⭐ | `EmbeddedKafka` 등을 활용한 통합 테스트 코드가 잘 갖춰져 있음. |

---

## 4. 종합 결론
`stockwellness-batch`는 단순히 데이터를 옮기는 수준을 넘어, **이벤트 기반 아키텍처(Kafka)와의 연동, AI 분석 파이프라인 통합, 정교한 보정(Repair) 로직**을 갖춘 고도화된 배치 시스템입니다. 위에서 언급한 로그 표준화 정도만 보완된다면 운영 안정성이 더욱 높아질 것으로 보입니다.

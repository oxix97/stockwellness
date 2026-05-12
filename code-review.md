# Code Review Report - API Server & GlobalExceptionHandler 연동

## 1. Intent
- `GlobalExceptionHandler`에서 발생하는 예기치 않은 시스템 에러(500)를 감지하여, 신규 Slack 알림 엔진(`SlackAlertService`)을 통해 실시간으로 알림을 전송하는 기능을 구현함.

## 2. Strengths
- **중앙 집중식 에러 관리**: `GlobalExceptionHandler`를 통해 처리되지 않은 모든 예외를 포괄적으로 캡처하고 알림을 보낼 수 있음.
- **풍부한 컨텍스트 제공**: 에러 발생 시 `traceId`, 요청 URL(쿼리 스트링 포함), 사용자 ID, 예외 타입, 스택 트레이스(5줄 제한) 등 문제 해결에 필요한 핵심 정보를 함께 전송함.
- **비동기 처리**: `SlackNotificationService`가 `@Async`로 구현되어 있어, 알림 전송 과정이 실제 API 응답 속도에 영향을 주지 않도록 설계됨.
- **Java 21 최신 문법 활용**: `GlobalExceptionHandler`에서 `switch` 표현식을 사용하여 가독성 높고 현대적인 코드를 작성함.

## 3. Issues & Opportunities for Improvement

### 🟡 IMPORTANT (MEDIUM/HIGH)
- **민감 정보 유출 위험 (PII Leakage)**: `request.getQueryString()`을 포함한 전체 URL을 Slack으로 전송하고 있음. 쿼리 파라미터에 사용자 토큰이나 개인정보가 포함될 경우 제3자 서비스인 Slack으로 해당 정보가 유출될 위험이 있음.
  - *Recommendation*: 민감한 파라미터를 필터링하거나, URL에서 쿼리 스트링을 제외하고 필수적인 정보만 마스킹하여 전송하는 로직이 필요함.
- **에러 메시지 내 민감 정보**: `e.getMessage()`를 그대로 Slack에 노출하고 있음. DB 쿼리 에러나 연동 시스템 에러 메시지에 민감한 인프라 정보나 파라미터가 포함될 수 있음.

### 🟢 MINOR (LOW)
- **타임존 하드코딩**: `SlackAlertService`에서 `ZoneId.of("Asia/Seoul")`을 직접 사용하고 있음. 프로젝트 설정 파일(properties)에서 관리하거나 시스템 기본값을 따르도록 개선 가능함.
- **Trace ID 연계**: `generateTraceId()`를 통해 매번 새로운 ID를 생성하고 있으나, 이미 요청에 Trace ID(MDC 등)가 부여되어 있다면 이를 재사용하여 전체 로그와 일관성을 유지하는 것이 좋음.
- **스택 트레이스 제한**: `MAX_STACK_TRACE_LINES = 5`는 Slack 메시지 길이를 고려한 적절한 선택이지만, 간혹 원인 파악에 부족할 수 있음. 핵심 비즈니스 로직 패키지를 우선적으로 추출하는 필터링 로직을 고려해볼 수 있음.

## 4. Assessment
- **Score: 90/100**
- 전반적으로 프로젝트의 기술 스택에 부합하는 깔끔하고 효율적인 구현임. 특히 비동기 알림 처리와 상세한 에러 컨텍스트 수집은 운영 단계에서 매우 유용할 것으로 판단됨. 보안 측면에서의 데이터 마스킹만 보완한다면 상용 수준의 견고한 알림 시스템이 될 것임.

---

# Code Review — Task 3 공통 SlackNotificationService 구현

## 요약
`NotificationContext`와 `SlackMessageBuilder`를 도입하여 구조화된 Slack Block Kit 메시지를 비동기로 전송하는 공통 서비스를 구현했습니다.

## 심각도 기준
- 🔴 BLOCKER: 머지 전 반드시 수정
- 🟡 MAJOR: 강력 권고 수정
- 🟢 MINOR: 선택적 개선

## 리뷰 항목

### 🟡 MAJOR
1. **모듈 간 의존성 및 빈 설정 불일치**: `SlackNotificationService`는 `core` 모듈에 위치하지만, 이를 위해 필요한 `alertExecutor`와 `slackRestClient` 빈은 `api` 모듈의 `ApiAsyncConfig`에만 정의되어 있습니다. 이로 인해 `batch` 모듈 등에서 해당 서비스를 주입받으려 할 때 `NoSuchBeanDefinitionException`이 발생하게 됩니다.
   - **권장 사항**: 해당 인프라 빈 설정(`Executor`, `RestClient`)을 `core` 모듈 내의 공통 설정 클래스(예: `org.stockwellness.config.SlackConfig`)로 이동하거나, 각 모듈에서 중복 정의되지 않도록 구조를 개선해야 합니다.

2. **Slack Block Kit 필드 개수 제한**: `SlackMessageBuilder`에서 `NotificationContext`의 `details`와 기본 필드들을 하나의 `section` 블록의 `fields` 배열에 모두 추가하고 있습니다. Slack API 명세상 한 `section` 내의 `fields`는 최대 10개까지만 허용됩니다. `details`가 많아질 경우 전송이 실패할 수 있습니다.
   - **권장 사항**: `fields`의 개수가 10개를 초과할 경우 여러 개의 `section` 블록으로 나누어 생성하도록 로직을 보완해야 합니다.

3. **테스트 커버리지 부족**: `SlackMessageBuilder`에 대한 단위 테스트는 잘 작성되어 있으나, `SlackNotificationService` 자체에 대한 테스트가 누락되었습니다. 비동기 동작 및 `RestClient` 호출 로직에 대한 검증이 필요합니다.

### 🟢 MINOR
1. **활성 프로필 표기 로직**: `env.getProperty("spring.profiles.active")`는 여러 프로필이 설정된 경우 콤마로 구분된 문자열(예: `dev,local`)을 반환합니다. 헤더에 표시할 때 첫 번째 프로필만 추출하거나 더 깔끔하게 포맷팅하는 것을 고려해 보세요.
2. **URI 생성 예외 처리**: `URI.create(properties.webhookUrl())` 호출 시 URL 형식이 잘못되면 `IllegalArgumentException`이 발생합니다. 현재는 `catch (Exception ex)`에서 처리되고 있으나, 빈 설정 시점이나 프로퍼티 로딩 시점에 유효성을 검증하는 것이 더 안전합니다.
3. **기존 서비스 정리**: `api` 모듈에 기존 `SlackAlertService`가 잔류하고 있습니다. 신규 서비스로 완전히 대체 가능하다면 `@Deprecated`를 붙이거나 삭제 일정을 확정하는 것이 좋습니다.

## 결론
구조적으로 잘 분리된 알림 엔진입니다. 다만, 공통 모듈(`core`)로서의 역할을 온전히 수행하기 위해 인프라 빈 설정의 위치를 조정하고, Slack API의 제약 사항(Max 10 fields)을 고려한 방어 로직을 추가하면 완벽할 것 같습니다.

**STATUS: REQUEST CHANGES (Major issues found)**

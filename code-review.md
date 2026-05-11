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

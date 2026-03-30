# SlackNotificationAdapter RestClient 리팩토링 및 오류 해결 계획

`stockwellness-batch` 애플리케이션 시작 시 `SlackNotificationAdapter`가 요구하는 `RestTemplate` 빈을 찾지 못해 발생하는 오류를 해결하고, 프로젝트의 기술 스택인 `RestClient`로 전환하기 위한 계획입니다.

## 문제 개요
- **증상**: `Application failed to start` - `Parameter 0 of constructor in SlackNotificationAdapter required a bean of type 'RestTemplate' that could not be found.`
- **원인**: 프로젝트에 `RestTemplate` 빈이 정의되어 있지 않음.
- **해결 방안**: 기존 `RestTemplate`을 사용하던 `SlackNotificationAdapter`를 프로젝트 표준인 `RestClient`로 리팩토링하여 일관성을 확보하고 주입 오류를 해결합니다.

## 변경 사항

### 1. stockwellness-batch
- `org.stockwellness.adapter.out.external.slack.SlackNotificationAdapter`
    - `RestTemplate` 필드를 제거하고 `RestClient`로 변경합니다.
    - 생성자에서 `RestClient.Builder`를 주입받아 `RestClient`를 초기화합니다.
    - `send` 메서드의 HTTP 요청 로직을 `RestClient` Fluent API 방식으로 변경합니다.

- `org.stockwellness.adapter.out.external.slack.SlackNotificationAdapterTest`
    - `RestTemplate` 대신 `RestClient`를 모킹하거나, `RestClient.Builder`를 활용하여 테스트할 수 있도록 수정합니다.

## 상세 구현 내용

### SlackNotificationAdapter.java (예상 변경 사항)
```java
@Component
public class SlackNotificationAdapter implements NotificationPort {
    private final RestClient restClient;
    private final String webhookUrl;

    public SlackNotificationAdapter(
        RestClient.Builder builder,
        @Value("${slack.webhook.url:}") String webhookUrl
    ) {
        this.restClient = builder.build();
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void send(String title, String content) {
        // ... 생략 ...
        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", message))
                .retrieve()
                .toBodilessEntity();
    }
}
```

## 검증 계획

### 1. 애플리케이션 시작 테스트
- `./gradlew :stockwellness-batch:bootRun` 명령을 실행하여 빈 주입 오류 없이 정상적으로 시작되는지 확인합니다.

### 2. 단위 테스트 실행
- `./gradlew :stockwellness-batch:test --tests "org.stockwellness.adapter.out.external.slack.SlackNotificationAdapterTest"` 실행하여 리팩토링된 로직의 정상 작동을 확인합니다.

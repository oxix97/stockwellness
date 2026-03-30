# SlackNotificationAdapter RestTemplate 빈 부재 문제 해결 계획

`stockwellness-batch` 애플리케이션 시작 시 `SlackNotificationAdapter`가 요구하는 `RestTemplate` 빈을 찾지 못해 발생하는 오류를 해결하기 위한 계획입니다.

## 문제 개요
- **증상**: `Application failed to start` - `Parameter 0 of constructor in org.stockwellness.adapter.out.external.slack.SlackNotificationAdapter required a bean of type 'org.springframework.web.client.RestTemplate' that could not be found.`
- **원인**: `SlackNotificationAdapter`는 `@Component`로 등록되어 `RestTemplate`을 주입받으려 하지만, 프로젝트 내에 해당 타입의 빈(Bean) 정의가 없습니다.

## 해결 방안
`stockwellness-core` 모듈에 공통으로 사용할 수 있는 `RestTemplate` 설정을 추가합니다.

## 변경 사항

### 1. stockwellness-core
- `org.stockwellness.config.RestTemplateConfig` 클래스 생성
    - `RestTemplateBuilder`를 사용하여 기본 타임아웃(Connect: 5s, Read: 5s)이 설정된 `RestTemplate` 빈을 정의합니다.

## 상세 단계

### 단계 1: RestTemplateConfig 생성
`stockwellness-core/src/main/java/org/stockwellness/config/RestTemplateConfig.java` 파일을 생성하고 다음 내용을 작성합니다:

```java
package org.stockwellness.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
```

## 검증 계획

### 1. 애플리케이션 시작 테스트
- `./gradlew :stockwellness-batch:bootRun` 명령을 실행하여 애플리케이션이 정상적으로 시작되는지 확인합니다.

### 2. 단위 테스트 실행
- 관련 테스트 클래스 실행: `./gradlew :stockwellness-batch:test --tests "org.stockwellness.adapter.out.external.slack.SlackNotificationAdapterTest"`

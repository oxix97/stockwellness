package org.stockwellness.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 프로젝트 전체에서 공통으로 사용할 RestClient 설정을 관리합니다.
 * Spring Boot 3.2+ 환경에서 RestClient.Builder 빈이 자동 등록되지만,
 * 배치 환경 등에서 명시적인 빈 등록이 필요한 경우를 위해 정의합니다.
 */
@Configuration
public class RestClientConfig {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}

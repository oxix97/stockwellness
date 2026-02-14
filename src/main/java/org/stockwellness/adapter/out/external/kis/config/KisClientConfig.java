package org.stockwellness.adapter.out.external.kis.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.adapter.KisTokenAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisProperties;

import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(KisProperties.class)
@RequiredArgsConstructor
public class KisClientConfig {

    private final KisProperties kisProperties;

    /**
     * [1] 인증용 클라이언트 (순수 토큰 발급용)
     * - Connection Pool 불필요 (호출 빈도 낮음)
     * - 단순 타임아웃만 적용하여 리소스 낭비 방지
     */
    @Bean("kisAuthClient")
    public RestClient kisAuthClient(RestClient.Builder builder) {
        return builder
                .baseUrl(kisProperties.baseUrl())
                .requestFactory(simpleClientFactory())
                .defaultHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }

    /**
     * [2] 메인 API 클라이언트 (시세 조회 등)
     * - Connection Pool 적용 (배치 성능 핵심)
     * - Token 자동 주입 Interceptor 적용
     * - 커스텀 ObjectMapper (Snake Case, 날짜 처리, Unknown Field 무시)
     */
    @Bean("kisApiClient")
    public RestClient kisApiClient(RestClient.Builder builder, KisTokenAdapter tokenAdapter) {
        return builder
                .baseUrl(kisProperties.baseUrl())
                .requestFactory(poolingClientFactory())
                .requestInterceptor(tokenAuthInterceptor(tokenAdapter))
                .defaultHeader("appkey", kisProperties.appKey())
                .defaultHeader("appsecret", kisProperties.appSecret())
                .defaultHeader("Content-Type", "application/json; charset=utf-8")
                // 4. 안전한 JSON 변환기 설정
                .messageConverters(converters ->
                        converters.addFirst(new MappingJackson2HttpMessageConverter(kisObjectMapper()))
                )
                .build();
    }

    // --- Helper Methods ---

    // Connection Pool Factory 생성 (Apache HttpClient 5)
    private ClientHttpRequestFactory poolingClientFactory() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(50); // 호스트당 최대 연결 수
        connectionManager.setMaxTotal(100); // 전체 최대 연결 수

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(5));
        return factory;
    }

    // 단순 타임아웃 팩토리 (인증용)
    private ClientHttpRequestFactory simpleClientFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(5));
        return factory;
    }

    // ObjectMapper 커스텀 (안전성 확보)
    private ObjectMapper kisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // API 변경 시 에러 방지
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    // 토큰 주입 Interceptor
    private ClientHttpRequestInterceptor tokenAuthInterceptor(KisTokenAdapter tokenAdapter) {
        return (request, body, execution) -> {
            String token = tokenAdapter.getAccessToken();
            request.getHeaders().add("Authorization", "Bearer " + token);
            return execution.execute(request, body);
        };
    }
}
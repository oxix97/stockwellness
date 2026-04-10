package org.stockwellness.adapter.out.external.kis.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.adapter.KisTokenAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisProperties;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(KisProperties.class)
@RequiredArgsConstructor
public class KisClientConfig {

    private static final Duration AUTH_READ_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration API_READ_TIMEOUT = Duration.ofSeconds(30);

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
                .requestFactory(bufferingRequestFactory(simpleClientFactory()))
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
                .requestFactory(bufferingRequestFactory(poolingClientFactory()))
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
        factory.setReadTimeout(API_READ_TIMEOUT);
        return factory;
    }

    // 단순 타임아웃 팩토리 (인증용)
    private ClientHttpRequestFactory simpleClientFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(AUTH_READ_TIMEOUT);
        return factory;
    }

    private ClientHttpRequestFactory bufferingRequestFactory(ClientHttpRequestFactory delegate) {
        return new BufferingClientHttpRequestFactory(delegate);
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
        return (request, body, execution) -> executeWithRetry(request, body, execution, tokenAdapter, false);
    }

    private ClientHttpResponse executeWithRetry(
            org.springframework.http.HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution,
            KisTokenAdapter tokenAdapter,
            boolean retried
    ) throws IOException {
        request.getHeaders().setBearerAuth(tokenAdapter.getAccessToken());

        ClientHttpResponse response = execution.execute(request, body);
        KisApiException error = inspectBusinessError(response);
        if (error == null) {
            return response;
        }

        if (error.isTokenExpired() && !retried) {
            log.warn("KIS 토큰 만료 응답 감지. 캐시 토큰을 폐기하고 1회 재시도합니다. msgCd={}, msg1={}", error.msgCd(), error.msg1());
            response.close();
            tokenAdapter.invalidateAccessToken();
            return executeWithRetry(request, body, execution, tokenAdapter, true);
        }

        log.error("KIS 업무 오류 응답 감지. 재시도 여부={}, msgCd={}, msg1={}", retried, error.msgCd(), error.msg1());
        throw error;
    }

    private KisApiException inspectBusinessError(ClientHttpResponse response) throws IOException {
        byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());
        if (!isJsonPayload(response.getHeaders(), responseBody)) {
            return null;
        }

        JsonNode root = kisObjectMapper().readTree(responseBody);
        if (root == null || !root.isObject()) {
            return null;
        }

        String rtCd = text(root, "rt_cd", "rtCd");
        if (rtCd == null || "0".equals(rtCd)) {
            return null;
        }

        String msgCd = text(root, "msg_cd", "msgCd");
        String msg1 = text(root, "msg1");
        return KisApiException.from(rtCd, msgCd, msg1);
    }

    private boolean isJsonPayload(HttpHeaders headers, byte[] responseBody) {
        MediaType contentType = headers.getContentType();
        if (contentType != null && (MediaType.APPLICATION_JSON.includes(contentType)
                || contentType.getSubtype().endsWith("+json"))) {
            return true;
        }

        String trimmed = new String(responseBody, StandardCharsets.UTF_8).trim();
        return trimmed.startsWith("{");
    }

    private String text(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.get(fieldName);
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        }
        return null;
    }
}

package org.stockwellness.adapter.out.external.kis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KisProperties.class)
@RequiredArgsConstructor
public class KisClientConfig {

    private final KisProperties kisProperties;

    /**
     * [1] 인증용 클라이언트 (순수 토큰 발급용)
     * - Interceptor 없음 (무한 루프 방지)
     * - 기본 Mapper 사용 (응답이 Snake Case여도 Record나 @JsonProperty로 처리 가능하므로 굳이 커스텀 불필요)
     */
    @Bean("kisAuthClient")
    public RestClient kisAuthClient(RestClient.Builder builder) {
        return builder
                .baseUrl(kisProperties.baseUrl())
                .build();
    }

    /**
     * [2] 메인 API 클라이언트 (시세 조회 등)
     * - Token 자동 주입 Interceptor 적용
     * - Snake Case 자동 변환 Mapper 적용
     */
    @Bean("kisApiClient")
    public RestClient kisApiClient(RestClient.Builder builder, KisTokenAdapter tokenAdapter) {
        return builder
                .baseUrl(kisProperties.baseUrl())
                .requestInterceptor(tokenAuthInterceptor(tokenAdapter))
                .defaultHeader("appkey", kisProperties.appKey())
                .defaultHeader("appsecret", kisProperties.appSecret())
                .defaultHeader("tr_cont", "N")
                .defaultHeader("custtype", "P")
                .messageConverters(converters ->
                        converters.addFirst(new MappingJackson2HttpMessageConverter(kisObjectMapper()))
                )
                .build();
    }

    private ObjectMapper kisObjectMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    // 토큰 주입 Interceptor
    private ClientHttpRequestInterceptor tokenAuthInterceptor(KisTokenAdapter tokenAdapter) {
        return (request, body, execution) -> {
            // Redis에서 토큰 조회 (없으면 갱신)
            String token = tokenAdapter.getAccessToken();
            request.getHeaders().add("Authorization", "Bearer " + token);
            return execution.execute(request, body);
        };
    }
}
package org.stockwellness.adapter.out.external.kis.adapter;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.fixture.StockFixture;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "kis.base-url=http://localhost:${wiremock.server.port}",
        "kis.app-key=test-key",
        "kis.app-secret=test-secret"
})
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
class KisApiClientWireMockTest {

    @Autowired
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private LettuceConnectionFactory redisConnectionFactory;

    @Test
    @DisplayName("만료된 KIS 토큰이면 캐시를 무효화하고 새 토큰으로 1회 재시도한다")
    void kisApiClient_WhenTokenExpired_ShouldInvalidateAndRetryOnce() {
        Stock samsung = StockFixture.createSamsung();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("stockwellness:kis:token")).willReturn("expired-token", null, null);

        stubFor(post(urlEqualTo("/oauth2/tokenP"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "access_token": "refreshed-token",
                                  "expires_in": 3600,
                                  "token_type": "Bearer"
                                }
                                """)));

        stubFor(get(urlPathEqualTo("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"))
                .withHeader("Authorization", equalTo("Bearer expired-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "rt_cd": "1",
                                  "msg_cd": "TOKEN_EXPIRED",
                                  "msg1": "기간이 만료된 token 입니다."
                                }
                                """)));

        stubFor(get(urlPathEqualTo("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"))
                .withHeader("Authorization", equalTo("Bearer refreshed-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "rt_cd": "0",
                                  "output1": { "hts_kor_isnm": "삼성전자" },
                                  "output2": [
                                    {
                                      "stck_bsop_date": "20240101",
                                      "stck_oprc": "70000",
                                      "stck_hgpr": "71000",
                                      "stck_lwpr": "69000",
                                      "stck_clpr": "70500",
                                      "acml_vol": "1000000",
                                      "acml_tr_pbmn": "70500000000",
                                      "prdy_vrss": "500",
                                      "prdy_vrss_sign": "2"
                                    }
                                  ]
                                }
                                """)));

        List<KisDailyPriceDetail> result = kisDailyPriceAdapter.fetchDailyPrices(
                samsung,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 1)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).closePrice()).isEqualByComparingTo("70500");
        verify(redisTemplate).delete("stockwellness:kis:token");
        verify(valueOperations).set("stockwellness:kis:token", "refreshed-token", Duration.ofSeconds(3300));
        WireMock.verify(1, postRequestedFor(urlEqualTo("/oauth2/tokenP")));
        WireMock.verify(1, getRequestedFor(urlPathEqualTo("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"))
                .withHeader("Authorization", equalTo("Bearer expired-token")));
        WireMock.verify(1, getRequestedFor(urlPathEqualTo("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"))
                .withHeader("Authorization", equalTo("Bearer refreshed-token")));
    }

    @Test
    @DisplayName("토큰 만료가 아닌 KIS 업무 오류는 즉시 예외를 던진다")
    void kisApiClient_WhenBusinessError_ShouldThrowException() {
        Stock samsung = StockFixture.createSamsung();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("stockwellness:kis:token")).willReturn("valid-token");

        stubFor(get(urlPathEqualTo("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"))
                .withHeader("Authorization", equalTo("Bearer valid-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "rt_cd": "1",
                                  "msg_cd": "INPUT_ERROR",
                                  "msg1": "입력값 오류"
                                }
                                """)));

        assertThatThrownBy(() -> kisDailyPriceAdapter.fetchDailyPrices(
                samsung,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 1)
        )).isInstanceOf(KisApiException.class)
                .hasMessageContaining("입력값 오류");

        verify(redisTemplate, never()).delete("stockwellness:kis:token");
    }

    @Test
    @DisplayName("KIS 초당 제한 오류 발생 시 설정된 횟수만큼 재시도한다")
    void kisApiClient_WhenRateLimitExceeded_ShouldRetry() {
        Stock samsung = StockFixture.createSamsung();
        String rateLimitToken = "rate-limit-token";
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("stockwellness:kis:token")).willReturn(rateLimitToken);

        stubFor(get(urlPathEqualTo("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"))
                .withHeader("Authorization", equalTo("Bearer " + rateLimitToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "rt_cd": "1",
                                  "msg_cd": null,
                                  "msg1": "초당 거래건수를 초과하였습니다."
                                }
                                """)));

        assertThatThrownBy(() -> kisDailyPriceAdapter.fetchDailyPrices(
                samsung,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 1)
        )).isInstanceOf(KisApiException.class)
                .hasMessageContaining("초당 거래건수를 초과하였습니다.");

        verify(redisTemplate, never()).delete("stockwellness:kis:token");
        // 기본 Retry 설정이 maxAttempts=3 이므로 3번 호출되어야 함
        WireMock.verify(3,
                getRequestedFor(urlPathEqualTo("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"))
                        .withHeader("Authorization", equalTo("Bearer " + rateLimitToken)));
    }
}

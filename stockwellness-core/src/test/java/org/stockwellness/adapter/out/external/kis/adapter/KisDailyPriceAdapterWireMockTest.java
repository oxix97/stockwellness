package org.stockwellness.adapter.out.external.kis.adapter;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.fixture.StockFixture;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "kis.base-url=http://localhost:${wiremock.server.port}",
        "kis.app-key=test-key",
        "kis.app-secret=test-secret"
})
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
class KisDailyPriceAdapterWireMockTest {

    @Autowired
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @MockitoBean
    private KisTokenAdapter kisTokenAdapter;

    @MockitoBean
    private LettuceConnectionFactory redisConnectionFactory;

    @Test
    @DisplayName("국내 주식 일별 시세를 KIS API로부터 정상적으로 가져온다")
    void fetchDailyPrices_ShouldReturnDataFromApi() {
        // 준비
        Stock samsung = StockFixture.createSamsung();
        given(kisTokenAdapter.getAccessToken()).willReturn("test-access-token");

        stubFor(get(urlPathEqualTo("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"))
                .withQueryParam("FID_INPUT_ISCD", equalTo("005930"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
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

        // 실행
        List<KisDailyPriceDetail> result = kisDailyPriceAdapter.fetchDailyPrices(
                samsung, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)
        );

        // 검증
        assertThat(result).hasSize(1);
        assertThat(result.get(0).baseDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(result.get(0).closePrice()).isEqualByComparingTo("70500");
    }
}

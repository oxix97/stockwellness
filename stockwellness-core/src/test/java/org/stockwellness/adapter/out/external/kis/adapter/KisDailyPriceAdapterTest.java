package org.stockwellness.adapter.out.external.kis.adapter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KisDailyPriceAdapterTest {

    private KisDailyPriceAdapter adapter;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("https://openapi.koreainvestment.com:9443").build();
        adapter = new KisDailyPriceAdapter(restClient, RateLimiter.ofDefaults("kisRateLimiter"));
    }

    @Test
    @DisplayName("국내 지수 일별 시세 조회는 날짜 파라미터를 전달하고 정상 응답을 매핑한다")
    void fetchIndexDailyPrices_success() {
        String indexCode = "0001";
        LocalDate startDate = LocalDate.of(2026, 4, 2);
        LocalDate endDate = LocalDate.of(2026, 4, 9);

        String mockResponse = """
                {
                  "rtCd": "0",
                  "msgCd": "MCA00000",
                  "msg1": "정상처리 되었습니다.",
                  "output2": [
                    {
                      "stck_bsop_date": "20260409",
                      "bstp_nmix_prpr": "2510.00",
                      "bstp_nmix_oprc": "2500.00",
                      "bstp_nmix_hgpr": "2520.00",
                      "bstp_nmix_lwpr": "2490.00",
                      "acml_vol": "120000",
                      "acml_tr_pbmn": "12000",
                      "bstp_nmix_prdy_vrss": "10.00",
                      "bstp_nmix_prdy_ctrt": "0.40"
                    },
                    {
                      "stck_bsop_date": "20260401",
                      "bstp_nmix_prpr": "2480.00",
                      "bstp_nmix_oprc": "2470.00",
                      "bstp_nmix_hgpr": "2490.00",
                      "bstp_nmix_lwpr": "2460.00",
                      "acml_vol": "100000",
                      "acml_tr_pbmn": "10000",
                      "bstp_nmix_prdy_vrss": "-5.00",
                      "bstp_nmix_prdy_ctrt": "-0.20"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(containsString("/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice")))
                .andExpect(requestTo(containsString("FID_INPUT_DATE_1=20260402")))
                .andExpect(requestTo(containsString("FID_INPUT_DATE_2=20260409")))
                .andExpect(requestTo(containsString("FID_INPUT_ISCD=0001")))
                .andExpect(header("tr_id", "FHKUP03500100"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        List<BenchmarkPriceData> result = adapter.fetchIndexDailyPrices(indexCode, startDate, endDate);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().baseDate()).isEqualTo(LocalDate.of(2026, 4, 9));
        assertThat(result.getFirst().closePrice()).isEqualByComparingTo("2510.00");
        mockServer.verify();
    }

    @Test
    @DisplayName("국내 지수 일별 시세 조회는 KIS 업무 오류를 예외로 전파한다")
    void fetchIndexDailyPrices_throwsWhenBusinessErrorOccurs() {
        String mockResponse = """
                {
                  "rtCd": "1",
                  "msgCd": "INPUT_ERROR",
                  "msg1": "입력값 오류"
                }
                """;

        mockServer.expect(requestTo(containsString("/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice")))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.fetchIndexDailyPrices("0001", LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 9)))
                .isInstanceOf(KisApiException.class)
                .hasMessageContaining("입력값 오류");
    }

    @Test
    @DisplayName("해외 지수 일별 시세 조회 성공 테스트")
    void fetchOverseasIndexDailyPrices_success() {
        // given
        String indexCode = "SPX";
        LocalDate startDate = LocalDate.of(2026, 3, 30);
        
        String mockResponse = """
                {
                  "output1": {
                    "ovrs_nmix_prpr": "5200.00"
                  },
                  "output2": [
                    {
                      "stck_bsop_date": "20260401",
                      "ovrs_nmix_prpr": "5250.00",
                      "ovrs_nmix_oprc": "5200.00",
                      "ovrs_nmix_hgpr": "5260.00",
                      "ovrs_nmix_lwpr": "5190.00",
                      "acml_vol": "1000",
                      "mod_yn": "0"
                    },
                    {
                      "stck_bsop_date": "20260331",
                      "ovrs_nmix_prpr": "5200.00",
                      "ovrs_nmix_oprc": "5180.00",
                      "ovrs_nmix_hgpr": "5210.00",
                      "ovrs_nmix_lwpr": "5170.00",
                      "acml_vol": "900",
                      "mod_yn": "0"
                    },
                    {
                      "stck_bsop_date": "20260329",
                      "ovrs_nmix_prpr": "5150.00",
                      "ovrs_nmix_oprc": "5140.00",
                      "ovrs_nmix_hgpr": "5160.00",
                      "ovrs_nmix_lwpr": "5130.00",
                      "acml_vol": "800",
                      "mod_yn": "0"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(containsString("/uapi/overseas-price/v1/quotations/inquire-daily-chartprice")))
                .andExpect(requestTo(containsString("FID_INPUT_DATE_1=20260330")))
                .andExpect(requestTo(containsString("FID_INPUT_DATE_2=20260401")))
                .andExpect(requestTo(containsString("FID_INPUT_ISCD=SPX")))
                .andExpect(header("tr_id", "FHKST03030100"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        // when
        LocalDate endDate = LocalDate.of(2026, 4, 1);
        List<BenchmarkPriceData> result = adapter.fetchOverseasIndexDailyPrices(indexCode, startDate, endDate);

        // then
        assertThat(result).hasSize(2); // 3/30 이후인 4/1, 3/31 데이터만 포함되어야 함
        assertThat(result.get(0).baseDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(result.get(1).baseDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        mockServer.verify();
    }

    @Test
    @DisplayName("해외 지수 일별 시세 조회는 KIS 업무 오류를 예외로 전파한다")
    void fetchOverseasIndexDailyPrices_throwsWhenBusinessErrorOccurs() {
        String mockResponse = """
                {
                  "rtCd": "1",
                  "msgCd": "NO_DATA",
                  "msg1": "조회 결과가 없습니다."
                }
                """;

        mockServer.expect(requestTo(containsString("/uapi/overseas-price/v1/quotations/inquire-daily-chartprice")))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.fetchOverseasIndexDailyPrices("SPX", LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 9)))
                .isInstanceOf(KisApiException.class)
                .hasMessageContaining("조회 결과가 없습니다.");
    }
}

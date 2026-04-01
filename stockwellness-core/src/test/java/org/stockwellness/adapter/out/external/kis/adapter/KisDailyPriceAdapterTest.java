package org.stockwellness.adapter.out.external.kis.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KisDailyPriceAdapterTest {

    private KisDailyPriceAdapter adapter;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("https://openapi.koreainvestment.com:9443").build();
        adapter = new KisDailyPriceAdapter(restClient);
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
}

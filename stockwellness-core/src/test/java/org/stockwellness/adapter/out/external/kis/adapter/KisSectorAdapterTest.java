package org.stockwellness.adapter.out.external.kis.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
import org.stockwellness.application.port.out.stock.InvestorTradingSnapshot;
import org.stockwellness.application.port.out.stock.SectorDailyDetailSnapshot;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KisSectorAdapterTest {

    private KisSectorAdapter adapter;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("https://openapi.koreainvestment.com:9443").build();
        RateLimiter rateLimiter = mock(RateLimiter.class);
        
        // Mock RateLimiter to execute the supplier
        when(rateLimiter.executeSupplier(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        adapter = new KisSectorAdapter(restClient, rateLimiter);
    }

    @Test
    @DisplayName("fetchTodaySectorDetail은 당일 상세와 수급 데이터를 함께 매핑한다")
    void fetchTodaySectorDetail_mapsDailyDetailAndSupply() {
        LocalDate today = LocalDate.of(2026, 4, 9);

        mockSectorDetail("0029");
        mockInvestorTrading("0029", "KSP", "20260409", "200", "100");

        SectorDailyDetailSnapshot result = adapter.fetchTodaySectorDetail("0029", today);

        assertThat(result.sectorCode()).isEqualTo("0029");
        assertThat(result.baseDate()).isEqualTo(today);
        assertThat(result.currentPrice()).isEqualByComparingTo(new BigDecimal("1000.12"));
        assertThat(result.changeAmount()).isEqualByComparingTo(new BigDecimal("12.34"));
        assertThat(result.changeRate()).isEqualByComparingTo(new BigDecimal("1.23"));
        assertThat(result.accumulatedVolume()).isEqualTo(123456L);
        assertThat(result.accumulatedTradingAmount()).isEqualTo(987654L);
        assertThat(result.risingIssueCount()).isEqualTo(10);
        assertThat(result.upperLimitIssueCount()).isEqualTo(1);
        assertThat(result.fallingIssueCount()).isEqualTo(3);
        assertThat(result.yearlyHighDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(result.yearlyLowDate()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(result.totalAskResidualQuantity()).isEqualTo(111L);
        assertThat(result.totalBidResidualQuantity()).isEqualTo(222L);
        assertThat(result.netBuyResidualQuantity()).isEqualTo(333L);
        assertThat(result.netForeignBuyAmount()).isEqualTo(100_000_000L);
        assertThat(result.netInstBuyAmount()).isEqualTo(200_000_000L);
        mockServer.verify();
    }

    @Test
    @DisplayName("fetchInvestorTradingDaily는 문서 기준 요청 파라미터와 응답 영업일을 사용한다")
    void fetchInvestorTradingDaily_mapsRequestAndBusinessDate() {
        LocalDate today = LocalDate.of(2026, 4, 9);
        mockInvestorTrading("0029", "KSP", "20260408", "321", "123");

        List<InvestorTradingSnapshot> result = adapter.fetchInvestorTradingDaily("0029", today, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).baseDate()).isEqualTo(LocalDate.of(2026, 4, 8));
        assertThat(result.get(0).netInstitutionalBuyingAmt()).isEqualByComparingTo("321000000");
        assertThat(result.get(0).netForeignBuyingAmt()).isEqualByComparingTo("123000000");
        mockServer.verify();
    }

    @Test
    @DisplayName("fetchInvestorTradingDaily는 공백 수급값을 0으로 보정한다")
    void fetchInvestorTradingDaily_normalizesBlankAmounts() {
        LocalDate today = LocalDate.of(2026, 4, 9);
        mockInvestorTrading("0013", "KSP", "20260409", "", "");

        List<InvestorTradingSnapshot> result = adapter.fetchInvestorTradingDaily("0013", today, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).netInstitutionalBuyingAmt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.get(0).netForeignBuyingAmt()).isEqualByComparingTo(BigDecimal.ZERO);
        mockServer.verify();
    }

    @Test
    @DisplayName("fetchInvestorTradingDaily는 output이 비어 있으면 빈 리스트를 반환한다")
    void fetchInvestorTradingDaily_returnsEmptyWhenOutputMissing() {
        String mockResponse = """
                {
                  "rtCd": "0",
                  "output": []
                }
                """;

        mockServer.expect(requestTo(containsString("/uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market")))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        assertThat(adapter.fetchInvestorTradingDaily("0029", LocalDate.of(2026, 4, 9), 1)).isEmpty();
    }

    @Test
    @DisplayName("fetchInvestorTradingDaily는 KIS 업무 오류를 빈 리스트로 삼키지 않고 예외를 던진다")
    void fetchInvestorTradingDaily_throwsWhenBusinessErrorOccurs() {
        String mockResponse = """
                {
                  "rtCd": "1",
                  "msgCd": "INPUT_ERROR",
                  "msg1": "입력값 오류"
                }
                """;

        mockServer.expect(requestTo(containsString("/uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market")))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.fetchInvestorTradingDaily("0029", LocalDate.of(2026, 4, 9), 1))
                .isInstanceOf(KisApiException.class)
                .hasMessageContaining("입력값 오류");
    }

    @Test
    @DisplayName("fetchInvestorTradingDaily는 1000 이상 2000 미만 indexCode에 KSQ를 사용한다")
    void fetchInvestorTradingDaily_usesKsqForKosdaqIndexCode() {
        LocalDate today = LocalDate.of(2026, 4, 9);
        mockInvestorTrading("1014", "KSQ", "20260409", "456", "123");

        List<InvestorTradingSnapshot> result = adapter.fetchInvestorTradingDaily("1014", today, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).netInstitutionalBuyingAmt()).isEqualByComparingTo("456000000");
        mockServer.verify();
    }

    @Test
    @DisplayName("fetchTodaySectorDetail은 output이 없으면 예외를 던진다")
    void fetchTodaySectorDetail_throwsWhenOutputMissing() {
        String mockResponse = """
                {
                  "rtCd": "0",
                  "output": null
                }
                """;

        mockServer.expect(requestTo(containsString("/uapi/domestic-stock/v1/quotations/inquire-index-category-price")))
                .andExpect(requestTo(containsString("FID_INPUT_ISCD=0029")))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.fetchTodaySectorDetail("0029", LocalDate.of(2026, 4, 9)))
                .isInstanceOf(SectorDomainException.class);
    }

    private void mockSectorDetail(String indexCode) {
        String mockResponse = """
                {
                  "rtCd": "0",
                  "output": {
                    "bstp_nmix_prpr": "1000.12",
                    "bstp_nmix_prdy_vrss": "12.34",
                    "prdy_vrss_sign": "2",
                    "bstp_nmix_prdy_ctrt": "1.23",
                    "acml_vol": "123456",
                    "prdy_vol": "120000",
                    "acml_tr_pbmn": "987654",
                    "prdy_tr_pbmn": "950000",
                    "bstp_nmix_oprc": "995.10",
                    "bstp_nmix_hgpr": "1010.00",
                    "bstp_nmix_lwpr": "990.00",
                    "ascn_issu_cnt": "10",
                    "uplm_issu_cnt": "1",
                    "stnr_issu_cnt": "2",
                    "down_issu_cnt": "3",
                    "lslm_issu_cnt": "0",
                    "dryy_bstp_nmix_hgpr": "1050.00",
                    "dryy_hgpr_vrss_prpr_rate": "-4.75",
                    "dryy_bstp_nmix_hgpr_date": "20260401",
                    "dryy_bstp_nmix_lwpr": "880.00",
                    "dryy_lwpr_vrss_prpr_rate": "13.65",
                    "dryy_bstp_nmix_lwpr_date": "20260102",
                    "total_askp_rsqn": "111",
                    "total_bidp_rsqn": "222",
                    "seln_rsqn_rate": "33.33",
                    "shnu_rsqn_rate": "66.67",
                    "ntby_rsqn": "333"
                  }
                }
                """;

        mockServer.expect(requestTo(containsString("/uapi/domestic-stock/v1/quotations/inquire-index-category-price")))
                .andExpect(requestTo(containsString("FID_INPUT_ISCD=" + indexCode)))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));
    }

    private void mockInvestorTrading(String sectorCode, String marketCode, String businessDate, String institutional, String foreign) {
        String mockResponse = """
                {
                  "rtCd": "0",
                  "output": [
                    {
                      "stck_bsop_date": "%s",
                      "orgn_ntby_tr_pbmn": "%s",
                      "frgn_ntby_tr_pbmn": "%s"
                    }
                  ]
                }
                """.formatted(businessDate, institutional, foreign);

        mockServer.expect(requestTo(containsString("/uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market")))
                .andExpect(requestTo(containsString("FID_COND_MRKT_DIV_CODE=U")))
                .andExpect(requestTo(containsString("FID_INPUT_DATE_1=20260409")))
                .andExpect(requestTo(containsString("FID_INPUT_DATE_2=20260309")))
                .andExpect(requestTo(containsString("FID_INPUT_ISCD=" + sectorCode)))
                .andExpect(requestTo(containsString("FID_INPUT_ISCD_1=" + marketCode)))
                .andExpect(requestTo(containsString("FID_INPUT_ISCD_2=" + sectorCode)))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));
    }
}

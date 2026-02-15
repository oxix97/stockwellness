package org.stockwellness.adapter.in.web.stock;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.application.port.in.stock.StockPriceUseCase;
import org.stockwellness.application.port.in.stock.StockSearchUseCase;
import org.stockwellness.application.port.in.stock.StockUseCase;
import org.stockwellness.application.port.in.stock.result.ChartDataResponse;
import org.stockwellness.application.port.in.stock.result.ReturnRateResponse;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.support.RestDocsSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockControllerTest extends RestDocsSupport {

    @MockitoBean
    private StockUseCase stockUseCase;

    @MockitoBean
    private StockPriceUseCase stockPriceUseCase;

    @MockitoBean
    private StockSearchUseCase stockSearchUseCase;

    @Test
    @DisplayName("종목 통합 검색 API")
    void searchStocks() throws Exception {
        // given
        StockSearchResult result = new StockSearchResult("005930", "삼성전자", MarketType.KOSPI, "전기전자", StockStatus.ACTIVE);
        given(stockUseCase.searchStocks(any())).willReturn(new SliceImpl<>(List.of(result)));

        // when & then
        mockMvc.perform(get("/api/v1/stocks/search")
                        .param("keyword", "삼성")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("stock-search",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Stock Discovery")
                                .summary("종목 통합 검색")
                                .description("티커 또는 종목명으로 주식을 검색합니다.")
                                .queryParameters(
                                        parameterWithName("keyword").description("검색어 (티커 또는 종목명)"),
                                        parameterWithName("marketType").description("마켓 타입 (KOSPI, KOSDAQ 등)").optional(),
                                        parameterWithName("status").description("종목 상태 (ACTIVE, HALTED 등)").optional(),
                                        parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                        parameterWithName("size").description("페이지 사이즈").optional()
                                )
                                .responseFields(
                                        fieldWithPath("content[].ticker").description("티커"),
                                        fieldWithPath("content[].name").description("종목명"),
                                        fieldWithPath("content[].marketType").description("마켓 타입"),
                                        fieldWithPath("content[].sectorName").description("섹터명"),
                                        fieldWithPath("content[].status").description("종목 상태"),
                                        fieldWithPath("pageable").description("페이징 정보"),
                                        fieldWithPath("last").description("마지막 페이지 여부"),
                                        fieldWithPath("numberOfElements").description("현재 페이지 엘리먼트 수"),
                                        fieldWithPath("first").description("첫 번째 페이지 여부"),
                                        fieldWithPath("size").description("페이지 사이즈"),
                                        fieldWithPath("number").description("현재 페이지 번호"),
                                        fieldWithPath("sort.empty").description("정렬 정보 비어있음 여부"),
                                        fieldWithPath("sort.sorted").description("정렬 여부"),
                                        fieldWithPath("sort.unsorted").description("미정렬 여부"),
                                        fieldWithPath("empty").description("결과 비어있음 여부")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("최근 검색어 조회 API")
    void getRecentSearches() throws Exception {
        // given
        given(stockSearchUseCase.getRecentSearches(any())).willReturn(List.of("삼성전자", "애플"));

        // when & then
        mockMvc.perform(get("/api/v1/stocks/search/history"))
                .andExpect(status().isOk())
                .andDo(document("stock-search-history",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Stock Discovery")
                                .summary("최근 검색어 조회")
                                .responseFields(
                                        fieldWithPath("[]").description("최근 검색어 목록")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("인기 검색어 조회 API")
    void getPopularSearches() throws Exception {
        // given
        given(stockSearchUseCase.getPopularSearches()).willReturn(List.of("삼성전자", "애플", "엔비디아"));

        // when & then
        mockMvc.perform(get("/api/v1/stocks/popular"))
                .andExpect(status().isOk())
                .andDo(document("stock-popular-search",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Stock Discovery")
                                .summary("인기 검색어 조회")
                                .responseFields(
                                        fieldWithPath("[]").description("인기 검색어 목록")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("신규 상장 종목 조회 API")
    void getNewListings() throws Exception {
        // given
        StockSearchResult result = new StockSearchResult("T1", "New Stock", MarketType.KOSPI, "기타", StockStatus.ACTIVE);
        given(stockUseCase.getNewListings()).willReturn(List.of(result));

        // when & then
        mockMvc.perform(get("/api/v1/stocks/new-listings"))
                .andExpect(status().isOk())
                .andDo(document("stock-new-listings",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Stock Discovery")
                                .summary("신규 상장 종목 조회")
                                .responseFields(
                                        fieldWithPath("[].ticker").description("티커"),
                                        fieldWithPath("[].name").description("종목명"),
                                        fieldWithPath("[].marketType").description("마켓 타입"),
                                        fieldWithPath("[].sectorName").description("섹터명"),
                                        fieldWithPath("[].status").description("종목 상태")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("최근 검색어 개별 삭제 API")
    void removeSearchHistory() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/stocks/search/history?keyword=삼성전자"))
                .andExpect(status().isNoContent())
                .andDo(document("stock-search-history-delete",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Stock Discovery")
                                .summary("최근 검색어 개별 삭제")
                                .queryParameters(
                                        parameterWithName("keyword").description("삭제할 검색어")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("최근 검색어 전체 삭제 API")
    void clearSearchHistory() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/stocks/search/history/all"))
                .andExpect(status().isNoContent())
                .andDo(document("stock-search-history-clear",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Stock Discovery")
                                .summary("최근 검색어 전체 삭제")
                                .build())
                ));
    }

    @Test
    @DisplayName("차트용 시세 데이터 조회 API")
    void getPriceHistory() throws Exception {
        // given
        ChartDataResponse.ChartPoint price = new ChartDataResponse.ChartPoint(
                LocalDate.of(2024, 1, 1),
                BigDecimal.valueOf(100), BigDecimal.valueOf(110),
                BigDecimal.valueOf(90), BigDecimal.valueOf(105),
                BigDecimal.valueOf(105), 1000L
        );
        ChartDataResponse response = new ChartDataResponse("AAPL", List.of(price), List.of());
        given(stockPriceUseCase.loadChartData(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/stocks/{ticker}/prices/history", "AAPL")
                        .param("period", "1Y")
                        .param("frequency", "DAILY"))
                .andExpect(status().isOk())
                .andDo(document("stock-price-history",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Stock Price")
                                .summary("차트용 시세 데이터 조회")
                                .pathParameters(parameterWithName("ticker").description("종목 티커"))
                                .queryParameters(
                                        parameterWithName("period").description("조회 기간 (1W, 1M, 1Y, ALL 등)").optional(),
                                        parameterWithName("frequency").description("데이터 주기 (DAILY, WEEKLY, MONTHLY)").optional(),
                                        parameterWithName("includeBenchmark").description("벤치마크 포함 여부").optional()
                                )
                                .responseFields(
                                        fieldWithPath("ticker").description("티커"),
                                        fieldWithPath("prices[].date").description("날짜"),
                                        fieldWithPath("prices[].open").description("시가"),
                                        fieldWithPath("prices[].high").description("고가"),
                                        fieldWithPath("prices[].low").description("저가"),
                                        fieldWithPath("prices[].close").description("종가"),
                                        fieldWithPath("prices[].adjClose").description("수정종가"),
                                        fieldWithPath("prices[].volume").description("거래량"),
                                        fieldWithPath("benchmarks").description("벤치마크 데이터 목록")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("수익률 조회 API")
    void getReturns() throws Exception {
        // given
        ReturnRateResponse response = new ReturnRateResponse("AAPL", "1Y", BigDecimal.valueOf(15.5), BigDecimal.valueOf(10.2));
        given(stockPriceUseCase.calculateReturn(eq("AAPL"), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/stocks/{ticker}/returns", "AAPL")
                        .param("period", "1Y"))
                .andExpect(status().isOk())
                .andDo(document("stock-returns",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Stock Price")
                                .summary("종목 수익률 조회")
                                .pathParameters(parameterWithName("ticker").description("종목 티커"))
                                .queryParameters(
                                        parameterWithName("period").description("조회 기간 (1W, 1M, 1Y 등)")
                                )
                                .responseFields(
                                        fieldWithPath("ticker").description("티커"),
                                        fieldWithPath("period").description("조회 기간"),
                                        fieldWithPath("stockReturnRate").description("종목 수익률 (%)"),
                                        fieldWithPath("benchmarkReturnRate").description("벤치마크 수익률 (%)")
                                )
                                .build())
                ));
    }
}

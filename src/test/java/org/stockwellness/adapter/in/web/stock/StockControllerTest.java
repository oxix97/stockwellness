package org.stockwellness.adapter.in.web.stock;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.SliceImpl;
import org.stockwellness.adapter.in.web.stock.dto.StockResponse;
import org.stockwellness.application.port.in.stock.StockAnalysisUseCase;
import org.stockwellness.application.port.in.stock.StockReadUseCase;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;
import org.stockwellness.fixture.StockFixture;
import org.stockwellness.support.RestDocsSupport;

import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import org.springframework.restdocs.payload.JsonFieldType;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Stock 컨트롤러 통합 테스트 (RestDocs)")
class StockControllerTest extends RestDocsSupport {

    @MockitoBean
    private StockReadUseCase stockReadUseCase;

    @MockitoBean
    private StockAnalysisUseCase stockAnalysisUseCase;

    @Nested
    @DisplayName("종목 조회 API")
    class Search {

        @Test
        @DisplayName("목록 조회: 검색 조건에 맞는 종목 리스트를 반환한다")
        void search_stocks() throws Exception {
            // given
            StockSearchResult result = StockFixture.createSearchResult(StockFixture.ISIN_CODE, StockFixture.NAME);
            given(stockReadUseCase.searchStocks(any(SearchStockQuery.class)))
                    .willReturn(new SliceImpl<>(List.of(result)));

            // when & then
            mockMvc.perform(get("/api/v1/stocks")
                            .param("keyword", "삼성")
                            .param("page", "1")
                            .param("size", "20")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andDo(document("stock-search",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Stock")
                                    .summary("종목 검색")
                                    .description("종목명 또는 티커로 종목을 검색합니다.")
                                    .queryParameters(
                                            parameterWithName("keyword").description("검색어 (종목명/티커)").optional(),
                                            parameterWithName("marketType").description("시장 구분 (KOSPI/KOSDAQ)").optional(),
                                            parameterWithName("status").description("상장 상태 (ACTIVE/DELISTED)").optional(),
                                            parameterWithName("page").description("페이지 번호").optional(),
                                            parameterWithName("size").description("페이지 크기").optional()
                                    )
                                    .build()),
                            relaxedResponseFields(
                                    fieldWithPath("content[].isinCode").description("ISIN 코드"),
                                    fieldWithPath("content[].ticker").description("티커"),
                                    fieldWithPath("content[].name").description("종목명"),
                                    fieldWithPath("content[].marketType").description("시장 구분"),
                                    fieldWithPath("content[].totalShares").description("상장주식수")
                            )
                    ));
        }

        @Test
        @DisplayName("상세 조회: 티커로 종목 상세 정보를 조회한다")
        void get_stock_detail() throws Exception {
            // given
            StockDetailResult result = StockFixture.createDetailResult(StockFixture.ISIN_CODE, StockFixture.NAME);
            given(stockReadUseCase.getStockDetail("005930")).willReturn(result);

            // when & then
            mockMvc.perform(get("/api/v1/stocks/{ticker}", "005930")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andDo(document("stock-get-detail",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Stock")
                                    .summary("종목 상세 조회")
                                    .pathParameters(
                                            parameterWithName("ticker").description("조회할 종목 티커")
                                    )
                                    .responseSchema(Schema.schema("StockDetailResponse"))
                                    .responseFields(
                                            fieldWithPath("isinCode").type(JsonFieldType.STRING).description("ISIN 코드"),
                                            fieldWithPath("ticker").type(JsonFieldType.STRING).description("티커"),
                                            fieldWithPath("name").type(JsonFieldType.STRING).description("종목명"),
                                            fieldWithPath("marketType").type(JsonFieldType.STRING).description("시장 구분"),
                                            fieldWithPath("sector").type(JsonFieldType.STRING).description("업종").optional(),
                                            fieldWithPath("totalShares").type(JsonFieldType.NUMBER).description("상장주식수"),
                                            fieldWithPath("baseDate").type(JsonFieldType.STRING).description("기준일자"),
                                            fieldWithPath("closePrice").type(JsonFieldType.NUMBER).description("종가"),
                                            fieldWithPath("priceChange").type(JsonFieldType.NUMBER).description("대비"),
                                            fieldWithPath("fluctuationRate").type(JsonFieldType.NUMBER).description("등락률"),
                                            fieldWithPath("openPrice").type(JsonFieldType.NUMBER).description("시가"),
                                            fieldWithPath("highPrice").type(JsonFieldType.NUMBER).description("고가"),
                                            fieldWithPath("lowPrice").type(JsonFieldType.NUMBER).description("저가"),
                                            fieldWithPath("volume").type(JsonFieldType.NUMBER).description("거래량"),
                                            fieldWithPath("tradingValue").type(JsonFieldType.NUMBER).description("거래대금"),
                                            fieldWithPath("marketCap").type(JsonFieldType.NUMBER).description("시가총액"),
                                            fieldWithPath("rsi14").type(JsonFieldType.NUMBER).description("RSI(14)").optional(),
                                            fieldWithPath("ma20").type(JsonFieldType.NUMBER).description("20일 이평선").optional()
                                    )
                                    .build())
                    ));
        }
    }
}

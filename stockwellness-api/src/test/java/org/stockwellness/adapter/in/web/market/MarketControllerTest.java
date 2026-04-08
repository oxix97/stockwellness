package org.stockwellness.adapter.in.web.market;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.application.port.in.stock.MarketIndexUseCase;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult.HistoryPoint;
import org.stockwellness.support.RestDocsSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Market 컨트롤러 명세 테스트")
class MarketControllerTest extends RestDocsSupport {

    @MockitoBean
    private MarketIndexUseCase marketIndexUseCase;

    @Test
    @DisplayName("시장 지수 목록 조회 API 명세 생성")
    void getMarketIndexes_docs() throws Exception {
        // given
        List<MarketIndexResult> results = List.of(
                new MarketIndexResult(
                        "0001",
                        "코스피 종합",
                        new BigDecimal("2550.12"),
                        new BigDecimal("1.23"),
                        new BigDecimal("31.05"),
                        List.of(
                                new HistoryPoint(LocalDate.of(2026, 4, 3), new BigDecimal("2550.12"))
                        )
                ),
                new MarketIndexResult(
                        "SPX",
                        "S&P 500",
                        new BigDecimal("5123.45"),
                        new BigDecimal("-0.52"),
                        new BigDecimal("-26.70"),
                        List.of(
                                new HistoryPoint(LocalDate.of(2026, 4, 3), new BigDecimal("5123.45"))
                        )
                )
        );
        given(marketIndexUseCase.getMarketIndexes()).willReturn(results);

        // when & then
        List<FieldDescriptor> responseFields = new ArrayList<>(commonResponseFields());
        responseFields.addAll(List.of(
                fieldWithPath("data[].ticker").description("지수 티커"),
                fieldWithPath("data[].name").description("지수명"),
                fieldWithPath("data[].currentPrice").description("현재 지수 값"),
                fieldWithPath("data[].fluctuationRate").description("전일 대비 등락률"),
                fieldWithPath("data[].fluctuationAmount").description("전일 대비 등락폭"),
                fieldWithPath("data[].history[]").description("최근 지수 히스토리"),
                fieldWithPath("data[].history[].date").description("히스토리 기준일"),
                fieldWithPath("data[].history[].close").description("종가")
        ));

        mockMvc.perform(get("/api/v1/market/indexes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].ticker").value("0001"))
                .andDo(document("market-indexes",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Market")
                                .summary("시장 지수 목록 조회")
                                .description("홈 화면과 대시보드에서 사용하는 주요 국내외 시장 지수 정보를 조회합니다.")
                                .responseSchema(Schema.schema("MarketIndexListResponse"))
                                .responseFields(responseFields)
                                .build())
                ));
    }
}

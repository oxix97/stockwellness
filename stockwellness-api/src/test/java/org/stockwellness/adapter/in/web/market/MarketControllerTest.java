package org.stockwellness.adapter.in.web.market;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.application.port.in.stock.MarketIndexUseCase;
import org.stockwellness.application.port.in.stock.result.MarketDashboardResult;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult.HistoryPoint;
import org.stockwellness.application.port.in.stock.result.MarketWeatherLevel;
import org.stockwellness.application.port.in.stock.result.MarketWeatherReason;
import org.stockwellness.application.port.in.stock.result.MarketWeatherResult;
import org.stockwellness.support.RestDocsSupport;
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
        MarketDashboardResult result = new MarketDashboardResult(
                List.of(
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
                ),
                new MarketWeatherResult(
                        MarketWeatherLevel.SUNNY,
                        "오늘의 증시는 맑음이에요",
                        "주요 지수가 고르게 오르며 투자심리가 비교적 안정적인 편이에요",
                        MarketWeatherReason.STEADY_ADVANCE,
                        LocalDate.of(2026, 4, 3)
                )
        );
        given(marketIndexUseCase.getMarketIndexes()).willReturn(result);

        // when & then
        List<FieldDescriptor> responseFields = new ArrayList<>(commonResponseFields());
        responseFields.addAll(List.of(
                fieldWithPath("data.indexes").description("시장 지수 리스트"),
                fieldWithPath("data.indexes[].ticker").description("지수 티커"),
                fieldWithPath("data.indexes[].name").description("지수명"),
                fieldWithPath("data.indexes[].currentPrice").description("현재 지수 값"),
                fieldWithPath("data.indexes[].fluctuationRate").description("전일 대비 등락률"),
                fieldWithPath("data.indexes[].fluctuationAmount").description("전일 대비 등락폭"),
                fieldWithPath("data.indexes[].history[]").description("최근 지수 히스토리"),
                fieldWithPath("data.indexes[].history[].date").description("히스토리 기준일"),
                fieldWithPath("data.indexes[].history[].close").description("종가"),
                fieldWithPath("data.weather.weatherLevel").description("시장 날씨 단계"),
                fieldWithPath("data.weather.weatherMessage").description("홈 헤더 메인 문구"),
                fieldWithPath("data.weather.weatherDescription").description("시장 분위기 보조 설명"),
                fieldWithPath("data.weather.reasonCode").description("문구 생성 근거 코드"),
                fieldWithPath("data.weather.asOfDate").description("시장 날씨 기준일")
        ));

        mockMvc.perform(get("/api/v1/market/indexes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.indexes").isArray())
                .andExpect(jsonPath("$.data.indexes[0].ticker").value("0001"))
                .andExpect(jsonPath("$.data.weather.weatherLevel").value("SUNNY"))
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

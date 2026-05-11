package org.stockwellness.adapter.in.web.insight;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.application.port.in.insight.MarketWeatherUseCase;
import org.stockwellness.application.service.insight.dto.MarketWeatherResponse;
import org.stockwellness.support.RestDocsSupport;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MarketWeather 컨트롤러 명세 테스트")
class MarketWeatherControllerTest extends RestDocsSupport {

    @MockitoBean
    private MarketWeatherUseCase marketWeatherUseCase;

    @Test
    @DisplayName("최신 시장 기상 정보를 조회한다")
    void getLatestWeather_docs() throws Exception {
        // given
        MarketWeatherResponse response = MarketWeatherResponse.builder()
                .baseDate(LocalDate.now())
                .marketType("KOSPI")
                .weatherScore(85)
                .weatherState("SUNNY")
                .weatherEmoji("☀️")
                .aiSummary("시장이 긍정적입니다.")
                .topSectors(List.of(
                        MarketWeatherResponse.SectorWeatherDto.builder()
                                .sectorCode("0001")
                                .sectorName("IT")
                                .score(90)
                                .emoji("☀️")
                                .build()
                ))
                .bottomSectors(List.of(
                        MarketWeatherResponse.SectorWeatherDto.builder()
                                .sectorCode("0002")
                                .sectorName("금융")
                                .score(30)
                                .emoji("🌧️")
                                .build()
                ))
                .build();

        given(marketWeatherUseCase.getLatestMarketWeather("KOSPI"))
                .willReturn(Optional.of(response));

        // when & then
        List<FieldDescriptor> responseFields = new ArrayList<>(commonResponseFields());
        responseFields.addAll(List.of(
                fieldWithPath("data.baseDate").description("기준일"),
                fieldWithPath("data.marketType").description("시장 타입"),
                fieldWithPath("data.weatherScore").description("기상 점수 (0-100)"),
                fieldWithPath("data.weatherState").description("기상 상태 (SUNNY, CLOUDY 등)"),
                fieldWithPath("data.weatherEmoji").description("상태 이모지"),
                fieldWithPath("data.aiSummary").description("AI 시장 요약 브리핑"),
                fieldWithPath("data.topSectors").description("상위 섹터 목록"),
                fieldWithPath("data.topSectors[].sectorCode").description("섹터 코드"),
                fieldWithPath("data.topSectors[].sectorName").description("섹터명"),
                fieldWithPath("data.topSectors[].score").description("섹터 점수"),
                fieldWithPath("data.topSectors[].emoji").description("섹터 이모지"),
                fieldWithPath("data.topSectors[].state").type(JsonFieldType.STRING).description("섹터 상태").optional(),
                fieldWithPath("data.topSectors[].aiTitle").type(JsonFieldType.STRING).description("섹터 AI 제목").optional(),
                fieldWithPath("data.topSectors[].aiInsight").type(JsonFieldType.STRING).description("섹터 AI 인사이트").optional(),
                fieldWithPath("data.bottomSectors").description("하위 섹터 목록"),
                fieldWithPath("data.bottomSectors[].sectorCode").description("섹터 코드"),
                fieldWithPath("data.bottomSectors[].sectorName").description("섹터명"),
                fieldWithPath("data.bottomSectors[].score").description("섹터 점수"),
                fieldWithPath("data.bottomSectors[].emoji").description("섹터 이모지"),
                fieldWithPath("data.bottomSectors[].state").type(JsonFieldType.STRING).description("섹터 상태").optional(),
                fieldWithPath("data.bottomSectors[].aiTitle").type(JsonFieldType.STRING).description("섹터 AI 제목").optional(),
                fieldWithPath("data.bottomSectors[].aiInsight").type(JsonFieldType.STRING).description("섹터 AI 인사이트").optional()
        ));

        mockMvc.perform(get("/api/v1/market-weather/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.weatherScore").value(85))
                .andDo(document("market-weather-latest",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Insight")
                                .summary("최신 시장 기상 조회")
                                .description("KOSPI 지수 및 주요 섹터의 점수를 분석한 기상 정보를 조회합니다.")
                                .responseSchema(Schema.schema("MarketWeatherResponse"))
                                .responseFields(responseFields)
                                .build())
                ));
    }
}

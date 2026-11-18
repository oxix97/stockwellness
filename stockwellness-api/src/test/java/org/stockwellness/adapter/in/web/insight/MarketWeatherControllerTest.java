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
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
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
                .baseDate(LocalDate.of(2026, 11, 18))
                .marketType("KOSPI")
                .weatherScore(60)
                .weatherState("PARTLY_CLOUDY")
                .weatherEmoji("🌥️")
                .aiSummary(null)
                .topSectors(List.of(
                        MarketWeatherResponse.SectorWeatherDto.builder()
                                .sectorCode("0001")
                                .sectorName("IT")
                                .score(90)
                                .state("CLEAR")
                                .emoji("☀️")
                                .build()
                ))
                .bottomSectors(List.of(
                        MarketWeatherResponse.SectorWeatherDto.builder()
                                .sectorCode("0002")
                                .sectorName("금융")
                                .score(30)
                                .state("RAINY")
                                .emoji("🌧️")
                                .build()
                ))
                .build();

        given(marketWeatherUseCase.getLatestMarketWeather("KOSPI"))
                .willReturn(Optional.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/market-weather/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketType").value("KOSPI"))
                .andExpect(jsonPath("$.data.weatherScore").value(60))
                .andExpect(jsonPath("$.data.aiSummary").doesNotExist())
                .andDo(document("market-weather-latest",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Insight")
                                .summary("최신 시장 기상 조회")
                                .description("요청한 시장(marketType 생략 시 KOSPI, 지원 값: KOSPI/KOSDAQ)의 최신 시장 기상 정보를 조회합니다. 데이터가 없거나 지원하지 않는 시장이면 data는 null을 반환합니다.")
                                .queryParameters(parameterWithName("marketType")
                                        .description("시장 구분 (KOSPI 또는 KOSDAQ, 기본값 KOSPI)")
                                        .optional())
                                .responseSchema(Schema.schema("MarketWeatherResponse"))
                                .responseFields(marketWeatherResponseFields())
                                .build())
                ));
    }

    @Test
    @DisplayName("KOSDAQ 시장 기상을 조회한다")
    void getLatestWeather_uses_requested_market_type() throws Exception {
        given(marketWeatherUseCase.getLatestMarketWeather("KOSDAQ"))
                .willReturn(Optional.of(MarketWeatherResponse.builder()
                        .baseDate(LocalDate.of(2026, 11, 18))
                        .marketType("KOSDAQ")
                        .weatherScore(42)
                        .weatherState("CLOUDY")
                        .weatherEmoji("⛅")
                        .aiSummary(null)
                        .topSectors(List.of(
                                MarketWeatherResponse.SectorWeatherDto.builder()
                                        .sectorCode("1001")
                                        .sectorName("반도체")
                                        .score(65)
                                        .state("PARTLY_CLOUDY")
                                        .emoji("🌥️")
                                        .build()
                        ))
                        .bottomSectors(List.of(
                                MarketWeatherResponse.SectorWeatherDto.builder()
                                        .sectorCode("1002")
                                        .sectorName("바이오")
                                        .score(20)
                                        .state("RAINY")
                                        .emoji("🌧️")
                                        .build()
                        ))
                        .build()));

        mockMvc.perform(get("/api/v1/market-weather/latest").param("marketType", "KOSDAQ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketType").value("KOSDAQ"))
                .andExpect(jsonPath("$.data.weatherScore").value(42))
                .andDo(document("market-weather-latest-kosdaq",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Insight")
                                .summary("최신 시장 기상 조회")
                                .description("요청한 시장(marketType 생략 시 KOSPI, 지원 값: KOSPI/KOSDAQ)의 최신 시장 기상 정보를 조회합니다. 데이터가 없거나 지원하지 않는 시장이면 data는 null을 반환합니다.")
                                .queryParameters(parameterWithName("marketType")
                                        .description("시장 구분 (KOSPI 또는 KOSDAQ, 기본값 KOSPI)")
                                        .optional())
                                .responseSchema(Schema.schema("MarketWeatherResponse"))
                                .responseFields(marketWeatherResponseFields())
                                .build())
                ));
    }

    @Test
    @DisplayName("시장 기상 데이터가 없거나 미지원 시장이면 data를 null로 반환한다")
    void getLatestWeather_returns_null_data_when_absent_docs() throws Exception {
        given(marketWeatherUseCase.getLatestMarketWeather("KOSPI"))
                .willReturn(Optional.empty());
        given(marketWeatherUseCase.getLatestMarketWeather("SPX"))
                .willReturn(Optional.empty());

        List<FieldDescriptor> emptyResponseFields = new ArrayList<>(commonResponseFields());
        emptyResponseFields.add(fieldWithPath("data").type(JsonFieldType.NULL)
                .description("시장 기상 정보 (데이터가 없거나 미지원 시장이면 null)").optional());

        mockMvc.perform(get("/api/v1/market-weather/latest").param("marketType", "KOSPI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andDo(document("market-weather-latest-empty",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Insight")
                                .summary("최신 시장 기상 조회")
                                .description("요청한 시장(marketType 생략 시 KOSPI, 지원 값: KOSPI/KOSDAQ)의 최신 시장 기상 정보를 조회합니다. 데이터가 없거나 지원하지 않는 시장이면 data는 null을 반환합니다.")
                                .queryParameters(parameterWithName("marketType")
                                        .description("시장 구분 (KOSPI 또는 KOSDAQ, 기본값 KOSPI)")
                                        .optional())
                                .responseSchema(Schema.schema("MarketWeatherResponse"))
                                .responseFields(emptyResponseFields)
                                .build())
                ));

        mockMvc.perform(get("/api/v1/market-weather/latest").param("marketType", "SPX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private List<FieldDescriptor> marketWeatherResponseFields() {
        List<FieldDescriptor> responseFields = new ArrayList<>(commonResponseFields());
        responseFields.addAll(List.of(
                fieldWithPath("data").type(JsonFieldType.OBJECT).description("시장 기상 정보 (데이터가 없거나 미지원 시장이면 null)").optional(),
                fieldWithPath("data.baseDate").description("기준일"),
                fieldWithPath("data.marketType").description("시장 타입 (KOSPI 또는 KOSDAQ)"),
                fieldWithPath("data.weatherScore").description("당일 최종 섹터 점수의 산술평균 (0-100)"),
                fieldWithPath("data.weatherState").description("기상 상태 (CLEAR, SUNNY, PARTLY_CLOUDY, CLOUDY, FOGGY, RAINY, STORMY)"),
                fieldWithPath("data.weatherEmoji").description("상태 이모지"),
                fieldWithPath("data.aiSummary").type(JsonFieldType.STRING)
                        .description("AI 시장 요약 브리핑 (canonical 분석이 없거나 미연동이면 null)").optional(),
                fieldWithPath("data.topSectors").description("상위 섹터 목록"),
                fieldWithPath("data.topSectors[].sectorCode").description("섹터 코드"),
                fieldWithPath("data.topSectors[].sectorName").description("섹터명"),
                fieldWithPath("data.topSectors[].score").description("섹터 점수"),
                fieldWithPath("data.topSectors[].emoji").description("섹터 이모지"),
                fieldWithPath("data.topSectors[].state").type(JsonFieldType.STRING).description("섹터 상태").optional(),
                fieldWithPath("data.topSectors[].aiTitle").type(JsonFieldType.STRING)
                        .description("섹터 AI 제목 (canonical 분석이 없거나 미연동이면 null)").optional(),
                fieldWithPath("data.topSectors[].aiInsight").type(JsonFieldType.STRING)
                        .description("섹터 AI 인사이트 (canonical 분석이 없거나 미연동이면 null)").optional(),
                fieldWithPath("data.bottomSectors").description("하위 섹터 목록"),
                fieldWithPath("data.bottomSectors[].sectorCode").description("섹터 코드"),
                fieldWithPath("data.bottomSectors[].sectorName").description("섹터명"),
                fieldWithPath("data.bottomSectors[].score").description("섹터 점수"),
                fieldWithPath("data.bottomSectors[].emoji").description("섹터 이모지"),
                fieldWithPath("data.bottomSectors[].state").type(JsonFieldType.STRING).description("섹터 상태").optional(),
                fieldWithPath("data.bottomSectors[].aiTitle").type(JsonFieldType.STRING)
                        .description("섹터 AI 제목 (canonical 분석이 없거나 미연동이면 null)").optional(),
                fieldWithPath("data.bottomSectors[].aiInsight").type(JsonFieldType.STRING)
                        .description("섹터 AI 인사이트 (canonical 분석이 없거나 미연동이면 null)").optional()
        ));
        return responseFields;
    }
}

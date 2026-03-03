package org.stockwellness.adapter.in.web.sector;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.application.port.in.stock.SectorInsightUseCase;
import org.stockwellness.application.port.in.stock.result.SectorDetailResult;
import org.stockwellness.application.port.in.stock.result.SectorRankingResult;
import org.stockwellness.application.port.in.stock.result.SectorSupplyResult;
import org.stockwellness.domain.stock.insight.LeadingStock;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.support.RestDocsSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SectorDashboard 컨트롤러 명세 테스트")
class SectorDashboardControllerTest extends RestDocsSupport {

    @MockitoBean
    private SectorInsightUseCase sectorInsightUseCase;

    @Test
    @DisplayName("섹터 수익률 랭킹 조회 API 명세 생성")
    void getSectorRanking_docs() throws Exception {
        // given
        List<SectorRankingResult> result = List.of(
                new SectorRankingResult("001", "종합", new BigDecimal("2500.50"), new BigDecimal("3.45"), false),
                new SectorRankingResult("002", "반도체", new BigDecimal("1200.30"), new BigDecimal("-1.20"), true)
        );

        given(sectorInsightUseCase.getTopSectorsByFluctuation(any(), any(), anyInt()))
                .willReturn(result);

        // when & then
        mockMvc.perform(get("/api/v1/sectors/ranking/fluctuation")
                        .param("date", "2026-02-26")
                        .param("marketType", "KOSPI")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andDo(document("sector-ranking",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Sector")
                                .summary("섹터 등락률 랭킹 조회")
                                .description("지정된 날짜의 섹터별 등락률 순위를 조회합니다.")
                                .queryParameters(
                                        parameterWithName("date").description("조회 날짜 (yyyy-MM-dd)").optional(),
                                        parameterWithName("marketType").description("시장 구분 (KOSPI, KOSDAQ)").optional(),
                                        parameterWithName("limit").description("조회 개수").optional()
                                )
                                .responseSchema(Schema.schema("SectorRankingResponse"))
                                .responseFields(
                                        fieldWithPath("[]").description("섹터 랭킹 리스트"),
                                        fieldWithPath("[].sectorCode").description("섹터 코드"),
                                        fieldWithPath("[].sectorName").description("섹터명"),
                                        fieldWithPath("[].currentPrice").description("현재 지수"),
                                        fieldWithPath("[].fluctuationRate").description("평균 등락률"),
                                        fieldWithPath("[].isOverheated").description("과열 여부")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("섹터 수급 랭킹 조회 API 명세 생성")
    void getSectorSupplyRanking_docs() throws Exception {
        // given
        List<SectorSupplyResult> result = List.of(
                new SectorSupplyResult("001", "종합", 500000000L, 1200000000L, 3, 5),
                new SectorSupplyResult("002", "반도체", -100000000L, 300000000L, 0, 2)
        );

        given(sectorInsightUseCase.getTopSectorsBySupply(any(), any(), anyInt()))
                .willReturn(result);

        // when & then
        mockMvc.perform(get("/api/v1/sectors/ranking/supply")
                        .param("date", "2026-02-26")
                        .param("marketType", "KOSPI")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andDo(document("sector-supply",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Sector")
                                .summary("섹터 수급 랭킹 조회")
                                .description("외국인/기관 순매수 금액 및 연속 매수 일수 기반의 섹터 순위를 조회합니다.")
                                .queryParameters(
                                        parameterWithName("date").description("조회 날짜 (yyyy-MM-dd)").optional(),
                                        parameterWithName("marketType").description("시장 구분 (KOSPI, KOSDAQ)").optional(),
                                        parameterWithName("limit").description("조회 개수").optional()
                                )
                                .responseSchema(Schema.schema("SectorSupplyResponse"))
                                .responseFields(
                                        fieldWithPath("[]").description("섹터 수급 리스트"),
                                        fieldWithPath("[].sectorCode").description("섹터 코드"),
                                        fieldWithPath("[].sectorName").description("섹터명"),
                                        fieldWithPath("[].netForeignBuyAmount").description("외국인 순매수 금액"),
                                        fieldWithPath("[].netInstBuyAmount").description("기관 순매수 금액"),
                                        fieldWithPath("[].foreignConsecutiveBuyDays").description("외국인 연속 매수 일수"),
                                        fieldWithPath("[].instConsecutiveBuyDays").description("기관 연속 매수 일수")
                                )
                                .build())
                ));
    }

    @Test
    @DisplayName("섹터 상세 조회 API 명세 생성")
    void getSectorDetail_docs() throws Exception {
        // given
        SectorDetailResult result = new SectorDetailResult(
                "001", "조선/해운", LocalDate.of(2026, 2, 26),
                new BigDecimal("2500.50"), new BigDecimal("3.45"),
                new TechnicalIndicators(new BigDecimal("2400.00"), new BigDecimal("2350.00"), null, null, new BigDecimal("65.5"), null, null),
                true, "현재 섹터는 과열 구간에 진입했습니다. 과매수 상태입니다.",
                List.of(new LeadingStock("삼성중공업", "010140", new BigDecimal("5.2"), 1500000L, new BigDecimal("12000000000")))
        );

        given(sectorInsightUseCase.getSectorDetail(anyString(), any()))
                .willReturn(result);

        // when & then
        mockMvc.perform(get("/api/v1/sectors/{sectorCode}", "001")
                        .param("date", "2026-02-26"))
                .andExpect(status().isOk())
                .andDo(document("sector-detail",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Sector")
                                .summary("섹터 상세 정보 조회")
                                .description("특정 섹터의 상세 인사이트, 기술적 지표, 주도주 정보를 조회합니다.")
                                .pathParameters(
                                        parameterWithName("sectorCode").description("섹터 코드")
                                )
                                .queryParameters(
                                        parameterWithName("date").description("조회 날짜 (yyyy-MM-dd)").optional()
                                )
                                .responseSchema(Schema.schema("SectorDetailResponse"))
                                .responseFields(
                                        fieldWithPath("sectorCode").description("섹터 코드"),
                                        fieldWithPath("sectorName").description("섹터명"),
                                        fieldWithPath("baseDate").description("기준 날짜"),
                                        fieldWithPath("currentPrice").description("현재 지수"),
                                        fieldWithPath("fluctuationRate").description("평균 등락률"),
                                        fieldWithPath("technicalIndicators").description("기술적 지표"),
                                        fieldWithPath("technicalIndicators.ma5").description("5일 이동평균선").optional(),
                                        fieldWithPath("technicalIndicators.ma20").description("20일 이동평균선").optional(),
                                        fieldWithPath("technicalIndicators.ma60").description("60일 이동평균선").optional(),
                                        fieldWithPath("technicalIndicators.ma120").description("120일 이동평균선").optional(),
                                        fieldWithPath("technicalIndicators.rsi14").description("RSI(14) 지표").optional(),
                                        fieldWithPath("technicalIndicators.macd").description("MACD").optional(),
                                        fieldWithPath("technicalIndicators.macdSignal").description("MACD Signal").optional(),
                                        fieldWithPath("isOverheated").description("과열 진단 여부"),
                                        fieldWithPath("diagnosisMessage").description("상세 진단 메시지"),
                                        fieldWithPath("leadingStocks[]").description("주도주 리스트"),
                                        fieldWithPath("leadingStocks[].ticker").description("주도주 티커"),
                                        fieldWithPath("leadingStocks[].name").description("주도주 명"),
                                        fieldWithPath("leadingStocks[].fluctuationRate").description("주도주 등락률"),
                                        fieldWithPath("leadingStocks[].tradeVolume").description("주도주 거래량"),
                                        fieldWithPath("leadingStocks[].transactionAmt").description("주도주 거래대금")
                                )
                                .build())
                ));
    }
}

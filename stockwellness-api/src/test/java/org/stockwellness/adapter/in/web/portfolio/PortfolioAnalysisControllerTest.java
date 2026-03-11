package org.stockwellness.adapter.in.web.portfolio;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.application.port.in.portfolio.PortfolioDiversificationUseCase;
import org.stockwellness.application.port.in.portfolio.PortfolioRebalancingUseCase;
import org.stockwellness.application.port.in.portfolio.PortfolioValuationUseCase;
import org.stockwellness.application.port.in.portfolio.result.PortfolioDiversificationResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;
import org.stockwellness.support.RestDocsSupport;
import org.stockwellness.support.annotation.MockMember;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Portfolio 분석 API 통합 테스트")
class PortfolioAnalysisControllerTest extends RestDocsSupport {

    @MockitoBean
    private PortfolioValuationUseCase portfolioValuationUseCase;

    @MockitoBean
    private PortfolioDiversificationUseCase portfolioDiversificationUseCase;

    @MockitoBean
    private PortfolioRebalancingUseCase portfolioRebalancingUseCase;

    @Test
    @MockMember(id = 1L)
    @DisplayName("가치 분석: 포트폴리오의 실시간 가치와 성과를 조회한다")
    void get_valuation() throws Exception {
        // given
        PortfolioValuationResult result = new PortfolioValuationResult(
                new BigDecimal("2000"),
                new BigDecimal("2100"),
                new BigDecimal("100"),
                new BigDecimal("5.00"),
                new BigDecimal("50"),
                new BigDecimal("2.44")
        );
        given(portfolioValuationUseCase.getValuation(1L, 100L)).willReturn(result);

        // when & then
        mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/analysis/valuation", 100L)
                        .header("Authorization", "Bearer {ACCESS_TOKEN}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("portfolio-analysis-valuation",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Portfolio Analysis")
                                .summary("포트폴리오 가치 및 수익률 분석")
                                .pathParameters(
                                        parameterWithName("portfolioId").description("포트폴리오 ID")
                                )
                                .responseSchema(Schema.schema("PortfolioValuationResponse"))
                                .responseFields(
                                        fieldWithPath("totalPurchaseAmount").description("총 매입 금액"),
                                        fieldWithPath("currentTotalValue").description("현재 총 가치"),
                                        fieldWithPath("totalProfitLoss").description("총 평가 손익"),
                                        fieldWithPath("totalReturnRate").description("총 수익률 (%)"),
                                        fieldWithPath("dailyProfitLoss").description("일일 평가 손익"),
                                        fieldWithPath("dailyReturnRate").description("일일 수익률 (%)")
                                )
                                .build())
                ));
    }

    @Test
    @MockMember(id = 1L)
    @DisplayName("비중 분석: 포트폴리오의 자산군, 업종, 국가 비중을 조회한다")
    void get_diversification() throws Exception {
        // given
        PortfolioDiversificationResult result = new PortfolioDiversificationResult(
                new BigDecimal("1000000"),
                Map.of("STOCK", new BigDecimal("70.00"), "CASH", new BigDecimal("30.00")),
                Map.of("IT", new BigDecimal("40.00"), "금융", new BigDecimal("30.00")),
                Map.of("KR", new BigDecimal("60.00"), "US", new BigDecimal("40.00"))
        );
        given(portfolioDiversificationUseCase.getDiversification(1L, 100L)).willReturn(result);

        // when & then
        mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/analysis/diversification", 100L)
                        .header("Authorization", "Bearer {ACCESS_TOKEN}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("portfolio-analysis-diversification",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Portfolio Analysis")
                                .summary("포트폴리오 비중 분석 (자산군/업종/국가)")
                                .pathParameters(
                                        parameterWithName("portfolioId").description("포트폴리오 ID")
                                )
                                .responseSchema(Schema.schema("PortfolioDiversificationResponse"))
                                .responseFields(
                                        fieldWithPath("totalValue").description("포트폴리오 총 가치"),
                                        fieldWithPath("assetRatios[].name").description("자산군 이름 (STOCK/CASH)"),
                                        fieldWithPath("assetRatios[].value").description("비중 (%)"),
                                        fieldWithPath("sectorRatios[].name").description("업종 이름"),
                                        fieldWithPath("sectorRatios[].value").description("비중 (%)"),
                                        fieldWithPath("countryRatios[].name").description("국가 코드 (KR/US/...)"),
                                        fieldWithPath("countryRatios[].value").description("비중 (%)")
                                )
                                .build())
                ));
    }

    @Test
    @MockMember(id = 1L)
    @DisplayName("리밸런싱: 목표 비중에 도달하기 위한 매매 가이드를 조회한다")
    void get_rebalancing() throws Exception {
        // given
        PortfolioRebalancingResult result = new PortfolioRebalancingResult(
                new BigDecimal("2000"),
                List.of(
                        new PortfolioRebalancingResult.RebalancingItem(
                                "AAPL", new BigDecimal("80"), new BigDecimal("60"), new BigDecimal("-20"),
                                new BigDecimal("10"), new BigDecimal("-2.5"), new BigDecimal("160")
                        )
                )
        );
        given(portfolioRebalancingUseCase.getRebalancingGuide(1L, 100L)).willReturn(result);

        // when & then
        mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/analysis/rebalancing", 100L)
                        .header("Authorization", "Bearer {ACCESS_TOKEN}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("portfolio-analysis-rebalancing",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Portfolio Analysis")
                                .summary("포트폴리오 리밸런싱 가이드")
                                .pathParameters(
                                        parameterWithName("portfolioId").description("포트폴리오 ID")
                                )
                                .responseSchema(Schema.schema("PortfolioRebalancingResponse"))
                                .responseFields(
                                        fieldWithPath("totalValue").description("포트폴리오 총 시장 가치"),
                                        fieldWithPath("items[].symbol").description("종목 코드"),
                                        fieldWithPath("items[].currentWeight").description("현재 비중 (%)"),
                                        fieldWithPath("items[].targetWeight").description("목표 비중 (%)"),
                                        fieldWithPath("items[].diffWeight").description("비중 차이 (목표 - 현재)"),
                                        fieldWithPath("items[].currentQuantity").description("현재 보유 수량"),
                                        fieldWithPath("items[].recommendedQuantity").description("추천 매매 수량 (+: 매수, -: 매도)"),
                                        fieldWithPath("items[].currentPrice").description("현재가"),
                                        fieldWithPath("items[].expectedTradeAmount").description("예상 매매 금액")
                                )
                                .build())
                ));
    }
}

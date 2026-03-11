package org.stockwellness.adapter.in.web.portfolio;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioItemRequest;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioUpdateRequest;
import org.stockwellness.adapter.out.external.ai.OpenAiAdapter;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.portfolio.result.AdviceResponse;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;
import org.stockwellness.domain.stock.*;
import org.stockwellness.application.service.portfolio.PortfolioFacade;
import org.stockwellness.fixture.PortfolioFixture;
import org.stockwellness.support.RestDocsSupport;
import org.stockwellness.support.annotation.MockMember;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("Portfolio 통합 테스트 (RestDocs)")
class PortfolioControllerTest extends RestDocsSupport {

    @Autowired
    PortfolioRepository portfolioRepository;

    @Autowired
    StockRepository stockRepository;

    @Autowired
    StockPriceRepository stockPriceRepository;

    @MockitoBean
    PortfolioFacade portfolioFacade;

    @Autowired
    private OpenAiAdapter openAiAdapter;

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @MockMember(id = 1L)
        @DisplayName("생성 및 조회: 포트폴리오를 생성하고 상세 조회한다")
        void create_and_get_portfolio() throws Exception {
            // [Given] 필수 종목 데이터 생성
            stockRepository.save(Stock.of(
                    "AAPL", "KR7005930003", "애플", MarketType.NASDAQ, Currency.USD, null, StockStatus.ACTIVE
            ));
            stockRepository.save(Stock.of(
                    "CASH", "CASH", "원화", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE
            ));

            // given
            List<PortfolioItemRequest> items = List.of(
                    new PortfolioItemRequest("AAPL", BigDecimal.TEN, BigDecimal.valueOf(150), "USD", AssetType.STOCK, BigDecimal.valueOf(60)),
                    new PortfolioItemRequest("CASH", BigDecimal.valueOf(500), BigDecimal.ONE, "USD", AssetType.CASH, BigDecimal.valueOf(40))
            );
            PortfolioCreateRequest request = new PortfolioCreateRequest(PortfolioFixture.NAME, PortfolioFixture.DESCRIPTION, items);

            given(portfolioFacade.createPortfolio(any())).willReturn(100L);
            given(portfolioFacade.getPortfolio(any(), any())).willReturn(org.stockwellness.application.port.in.portfolio.dto.PortfolioResponse.from(PortfolioFixture.createEntity(100L)));

            // when (Create)
            mockMvc.perform(post("/api/v1/portfolios")
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // when (Get)
            mockMvc.perform(get("/api/v1/portfolios/{id}", 100L)
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(PortfolioFixture.NAME));
        }

        @Test
        @MockMember(id = 1L)
        @DisplayName("조언 조회: 최신 AI 리밸런싱 조언을 조회한다")
        void get_latest_advice() throws Exception {
            // given
            Long portfolioId = 100L;
            AdviceResponse response = new AdviceResponse(
                    "상세 조언 내용입니다.",
                    AdviceAction.REBALANCE,
                    LocalDateTime.now()
            );

            given(portfolioFacade.getLatestAdvice(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/advice/latest", portfolioId)
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("상세 조언 내용입니다."))
                    .andExpect(jsonPath("$.action").value("REBALANCE"))
                    .andDo(document("portfolio-advice-latest",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("최신 AI 리밸런싱 조언 조회")
                                    .pathParameters(
                                            parameterWithName("portfolioId").description("포트폴리오 ID")
                                    )
                                    .responseFields(
                                            fieldWithPath("content").description("상세 조언 내용"),
                                            fieldWithPath("action").description("핵심 조언 액션 (REBALANCE, RISK_MANAGEMENT, TECHNICAL_OPTIMIZATION, DIVERSIFICATION)"),
                                            fieldWithPath("createdAt").description("생성 일시")
                                    )
                                    .build())));
        }
    }
}

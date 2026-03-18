package org.stockwellness.adapter.in.web.portfolio;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioItemRequest;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.in.portfolio.result.AdviceResponse;
import org.stockwellness.application.service.portfolio.PortfolioFacade;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.fixture.PortfolioFixture;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.support.RestDocsSupport;
import org.stockwellness.support.annotation.MockMember;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@DisplayName("Portfolio 통합 테스트 (RestDocs)")
class PortfolioControllerTest extends RestDocsSupport {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @MockitoBean
    private PortfolioFacade portfolioFacade;

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @MockMember(id = 1L)
        @DisplayName("생성: 포트폴리오를 생성한다")
        void create_portfolio() throws Exception {
            // [Given] 필수 종목 데이터 생성
            stockRepository.save(Stock.of("AAPL", "KR7005930003", "애플", MarketType.NASDAQ, Currency.USD, null, StockStatus.ACTIVE));
            stockRepository.save(Stock.of("CASH", "CASH", "원화", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE));

            List<PortfolioItemRequest> items = List.of(
                    new PortfolioItemRequest("AAPL", BigDecimal.TEN, BigDecimal.valueOf(150), "USD", AssetType.STOCK, BigDecimal.valueOf(60)),
                    new PortfolioItemRequest("CASH", BigDecimal.valueOf(500), BigDecimal.ONE, "USD", AssetType.CASH, BigDecimal.valueOf(40))
            );
            PortfolioCreateRequest request = new PortfolioCreateRequest(PortfolioFixture.NAME, PortfolioFixture.DESCRIPTION, items);

            given(portfolioFacade.createPortfolio(any())).willReturn(100L);

            mockMvc.perform(post("/api/v1/portfolios")
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andDo(document("portfolio-create",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("포트폴리오 생성")
                                    .requestFields(
                                            fieldWithPath("name").description("포트폴리오 이름"),
                                            fieldWithPath("description").description("포트폴리오 설명"),
                                            fieldWithPath("items[].symbol").description("종목 심볼"),
                                            fieldWithPath("items[].quantity").description("보유 수량"),
                                            fieldWithPath("items[].purchasePrice").description("평균 매수가"),
                                            fieldWithPath("items[].currency").description("통화 (KRW, USD)"),
                                            fieldWithPath("items[].assetType").description("자산 타입 (STOCK, CASH)"),
                                            fieldWithPath("items[].targetWeight").description("목표 비중 (%)")
                                    )
                                    .responseFields(new ArrayList<>(commonResponseFields()) {{
                                        add(fieldWithPath("data").description("생성된 포트폴리오 ID"));
                                    }})
                                    .build())));
        }

        @Test
        @MockMember(id = 1L)
        @DisplayName("조회: 포트폴리오 상세 정보를 조회한다")
        void get_portfolio() throws Exception {
            // given
            given(portfolioFacade.getPortfolio(any(), eq(100L))).willReturn(PortfolioResponse.from(PortfolioFixture.createEntity(100L)));

            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}", 100L)
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andDo(document("portfolio-get",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("포트폴리오 상세 조회")
                                    .pathParameters(parameterWithName("portfolioId").description("포트폴리오 ID"))
                                    .responseFields(new ArrayList<>(commonResponseFields()) {{
                                        add(fieldWithPath("data.id").description("포트폴리오 ID"));
                                        add(fieldWithPath("data.name").description("이름"));
                                        add(fieldWithPath("data.description").description("설명"));
                                        add(fieldWithPath("data.totalPurchaseAmount").description("총 매수 금액"));
                                        add(subsectionWithPath("data.items").description("포트폴리오 구성 종목 목록"));
                                    }})
                                    .build())));
        }

        @Test
        @MockMember(id = 1L)
        @DisplayName("조언 조회: 최신 AI 리밸런싱 조언을 조회한다")
        void get_latest_advice() throws Exception {
            // given
            AdviceResponse response = new AdviceResponse("상세 조언 내용입니다.", AdviceAction.REBALANCE, LocalDateTime.now());
            given(portfolioFacade.getLatestAdvice(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/advice/latest", 100L)
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andDo(document("portfolio-advice-latest",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("최신 AI 리밸런싱 조언 조회")
                                    .pathParameters(parameterWithName("portfolioId").description("포트폴리오 ID"))
                                    .responseFields(new ArrayList<>(commonResponseFields()) {{
                                        add(fieldWithPath("data.content").description("상세 조언 내용"));
                                        add(fieldWithPath("data.action").description("핵심 조언 액션"));
                                        add(fieldWithPath("data.createdAt").description("생성 일시"));
                                    }})
                                    .build())));
        }
    }
}

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
import org.stockwellness.application.port.in.portfolio.dto.PortfolioUpdateRequest;
import org.stockwellness.application.port.in.portfolio.result.AdviceResponse;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;
import org.stockwellness.application.service.portfolio.PortfolioFacade;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;
import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
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
            given(portfolioFacade.getPortfolio(any(), eq(100L))).willReturn(PortfolioResponse.from(PortfolioFixture.createEntity(100L), Collections.emptyMap()));

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
                                        add(fieldWithPath("data.currentTotalValue").description("총 평가 금액"));
                                        add(fieldWithPath("data.totalReturnRate").description("총 수익률"));
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

        @Test
        @MockMember(id = 1L)
        @DisplayName("목록 조회: 내 포트폴리오 목록을 조회한다")
        void get_my_portfolios() throws Exception {
            // given
            PortfolioResponse response = PortfolioResponse.from(PortfolioFixture.createEntity(100L), Collections.emptyMap());
            given(portfolioFacade.getMyPortfolios(any())).willReturn(List.of(response));

            mockMvc.perform(get("/api/v1/portfolios")
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andDo(document("portfolio-list",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("내 포트폴리오 목록 조회")
                                    .responseFields(new ArrayList<>(commonResponseFields()) {{
                                        add(fieldWithPath("data[].id").description("포트폴리오 ID"));
                                        add(fieldWithPath("data[].name").description("이름"));
                                        add(fieldWithPath("data[].description").description("설명"));
                                        add(fieldWithPath("data[].totalPurchaseAmount").description("총 매수 금액"));
                                        add(fieldWithPath("data[].currentTotalValue").description("총 평가 금액"));
                                        add(fieldWithPath("data[].totalReturnRate").description("총 수익률"));
                                        add(subsectionWithPath("data[].items").description("포트폴리오 구성 종목 목록"));
                                    }})
                                    .build())));
        }

        @Test
        @MockMember(id = 1L)
        @DisplayName("진단: 포트폴리오 건강 상태를 진단한다")
        void diagnose_portfolio() throws Exception {
            // given
            Map<String, Integer> categories = Map.of(
                    DiagnosisCategory.STABILITY.getKey(), 80,
                    DiagnosisCategory.RETURN.getKey(), 70,
                    DiagnosisCategory.AGILITY.getKey(), 90,
                    DiagnosisCategory.DIVERSIFICATION.getKey(), 85,
                    DiagnosisCategory.CASH.getKey(), 60
            );
            PortfolioHealthResult result = new PortfolioHealthResult(
                    77, categories, List.of(), 
                    BigDecimal.valueOf(15.5), BigDecimal.valueOf(5.2), BigDecimal.valueOf(1.2), BigDecimal.valueOf(0.5),
                    "Summary", "Insight", List.of("Step 1")
            );
            given(portfolioFacade.diagnosePortfolio(any(), any())).willReturn(result);

            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/health", 100L)
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andDo(document("portfolio-diagnose",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("포트폴리오 건강 진단")
                                    .pathParameters(parameterWithName("portfolioId").description("포트폴리오 ID"))
                                    .responseFields(new ArrayList<>(commonResponseFields()) {{
                                        add(fieldWithPath("data.overallScore").description("종합 점수"));
                                        add(subsectionWithPath("data.categories").description("카테고리별 점수 (Map)"));
                                        add(subsectionWithPath("data.stockContributions").description("종목별 기여도 목록"));
                                        add(fieldWithPath("data.mdd").description("최대 낙폭 (MDD)"));
                                        add(fieldWithPath("data.relativeMdd").description("벤치마크 대비 추가 하락폭"));
                                        add(fieldWithPath("data.sharpeRatio").description("샤프 지수"));
                                        add(fieldWithPath("data.alpha").description("초과 수익률 (Alpha)"));
                                        add(fieldWithPath("data.summary").description("진단 요약"));
                                        add(fieldWithPath("data.insight").description("상세 인사이트"));
                                        add(fieldWithPath("data.nextSteps").description("향후 조치 단계"));
                                    }})
                                    .build())));
        }

        @Test
        @MockMember(id = 1L)
        @DisplayName("수정: 포트폴리오 구성을 수정한다")
        void update_portfolio() throws Exception {
            // given
            PortfolioItemRequest itemRequest = new PortfolioItemRequest("005930", BigDecimal.valueOf(10), BigDecimal.valueOf(50000), "KRW", AssetType.STOCK, BigDecimal.valueOf(100));
            PortfolioUpdateRequest request = new PortfolioUpdateRequest("수정된 이름", "수정된 설명", List.of(itemRequest));

            mockMvc.perform(put("/api/v1/portfolios/{portfolioId}", 100L)
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andDo(document("portfolio-update",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("포트폴리오 수정")
                                    .pathParameters(parameterWithName("portfolioId").description("포트폴리오 ID"))
                                    .requestFields(
                                            fieldWithPath("name").description("수정할 이름"),
                                            fieldWithPath("description").description("수정할 설명"),
                                            fieldWithPath("items[].symbol").description("종목 심볼"),
                                            fieldWithPath("items[].quantity").description("보유 수량"),
                                            fieldWithPath("items[].purchasePrice").description("평균 매수가"),
                                            fieldWithPath("items[].currency").description("통화"),
                                            fieldWithPath("items[].assetType").description("자산 타입"),
                                            fieldWithPath("items[].targetWeight").description("목표 비중")
                                    )
                                    .responseFields(commonResponseFieldsWithNoData())
                                    .build())));
        }

        @Test
        @MockMember(id = 1L)
        @DisplayName("삭제: 포트폴리오를 삭제한다")
        void delete_portfolio() throws Exception {
            mockMvc.perform(delete("/api/v1/portfolios/{portfolioId}", 100L)
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .with(csrf())
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andDo(document("portfolio-delete",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("포트폴리오 삭제")
                                    .pathParameters(parameterWithName("portfolioId").description("포트폴리오 ID"))
                                    .responseFields(commonResponseFieldsWithNoData())
                                    .build())));
        }
    }
}

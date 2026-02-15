//package org.stockwellness.adapter.in.web.portfolio;
//
//import com.epages.restdocs.apispec.ResourceSnippetParameters;
//import com.epages.restdocs.apispec.Schema;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.transaction.annotation.Transactional;
//import org.stockwellness.application.port.in.portfolio.dto.PortfolioCreateRequest;
//import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioItemRequest;
//import org.stockwellness.application.port.in.portfolio.dto.PortfolioUpdateRequest;
//import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
//import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
//import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
//import org.stockwellness.application.port.in.portfolio.result.PortfolioAiResult;
//import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
//import org.stockwellness.application.port.out.stock.LlmClientPort;
//import org.stockwellness.domain.portfolio.AssetType;
//import org.stockwellness.domain.portfolio.Portfolio;
//import org.stockwellness.domain.portfolio.PortfolioItem;
//import org.stockwellness.fixture.PortfolioFixture;
//import org.stockwellness.support.RestDocsSupport;
//import org.stockwellness.support.annotation.MockMember;
//
//import java.util.List;
//
//import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
//import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
//import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
//import static org.hamcrest.Matchers.is;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.springframework.http.MediaType.APPLICATION_JSON;
//import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
//import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
//import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@Transactional
//@DisplayName("Portfolio 통합 테스트 (RestDocs)")
//class PortfolioControllerTest extends RestDocsSupport {
//
//    @Autowired
//    PortfolioRepository portfolioRepository;
//
//    @Autowired
//    StockRepository stockRepository;
//
//    @Autowired
//    StockPriceRepository StockPriceRepository;
//
//    @MockitoBean
//    LoadPortfolioAiPort loadPortfolioAiPort;
//
//    @MockitoBean
//    LlmClientPort llmClientPort;
//
//    static final Long MY_ID = 1L;
//    static final Long OTHER_ID = 99L;
//
//    @Nested
//    @DisplayName("성공 케이스")
//    class Success {
//
//        @Test
//        @MockMember(id = 1L)
//        @DisplayName("생성 및 조회: 포트폴리오를 생성하고 상세 조회한다")
//        void create_and_get_portfolio() throws Exception {
//            // [Given] 필수 종목 데이터 생성
//            stockRepository.save(org.stockwellness.domain.stock.Stock.of(
//                    "AAPL", "애플", "AAPL", org.stockwellness.domain.stock.MarketType.KOSPI, 1000000L, "123", "Apple Inc."
//            ));
//            stockRepository.save(org.stockwellness.domain.stock.Stock.of(
//                    "TSLA", "테슬라", "TSLA", org.stockwellness.domain.stock.MarketType.KOSPI, 500000L, "456", "Tesla Inc."
//            ));
//            stockRepository.save(org.stockwellness.domain.stock.Stock.of(
//                    "KRW", "원화", "KRW", org.stockwellness.domain.stock.MarketType.ETC, 0L, "000", "Korea Mint"
//            ));
//
//            // given
//            List<PortfolioItemRequest> items = List.of(
//                    PortfolioFixture.createItemRequest("AAPL", 4, AssetType.STOCK),
//                    PortfolioFixture.createItemRequest("KRW", 4, AssetType.CASH)
//            );
//            PortfolioCreateRequest request = PortfolioFixture.createCreateRequest(items);
//
//            // when (Create)
//            mockMvc.perform(post("/api/v1/portfolios")
//                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
//                            .with(csrf())
//                            .contentType(APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isCreated())
//                    .andDo(document("portfolio-create",
//                            resource(ResourceSnippetParameters.builder()
//                                    .tag("Portfolio")
//                                    .summary("포트폴리오 생성")
//                                    .description("새로운 투자 포트폴리오를 생성합니다.")
//                                    .requestSchema(Schema.schema("PortfolioCreateRequest"))
//                                    .requestHeaders(
//                                            headerWithName("Authorization").description("Bearer Access Token")
//                                    )
//                                    .requestFields(
//                                            fieldWithPath("name").description("포트폴리오 이름"),
//                                            fieldWithPath("description").description("설명"),
//                                            fieldWithPath("items[].stockCode").description("종목 코드 (ISIN)"),
//                                            fieldWithPath("items[].pieceCount").description("보유 조각 수"),
//                                            fieldWithPath("items[].assetType").description("자산 타입 (STOCK/CASH)")
//                                    )
//                                    .build())
//                    ));
//
//            Portfolio saved = portfolioRepository.findAll().stream()
//                    .filter(p -> p.getName().equals(PortfolioFixture.NAME))
//                    .findFirst().orElseThrow();
//
//            // when (Get)
//            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}", saved.getId())
//                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
//                            .contentType(APPLICATION_JSON))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.name", is(PortfolioFixture.NAME)))
//                    .andDo(document("portfolio-get",
//                            resource(ResourceSnippetParameters.builder()
//                                    .tag("Portfolio")
//                                    .summary("포트폴리오 상세 조회")
//                                    .pathParameters(
//                                            parameterWithName("portfolioId").description("조회할 포트폴리오 ID")
//                                    )
//                                    .requestHeaders(
//                                            headerWithName("Authorization").description("Bearer Access Token")
//                                    )
//                                    .responseSchema(Schema.schema("PortfolioResponse"))
//                                    .responseFields(
//                                            fieldWithPath("id").description("포트폴리오 ID"),
//                                            fieldWithPath("name").description("포트폴리오 이름"),
//                                            fieldWithPath("description").description("설명"),
//                                            fieldWithPath("totalPieces").description("총 조각 수"),
//                                            fieldWithPath("items[].stockCode").description("종목 코드"),
//                                            fieldWithPath("items[].pieceCount").description("조각 수"),
//                                            fieldWithPath("items[].assetType").description("자산 타입"),
//                                            fieldWithPath("items[].piece").description("조각 (Deprecated 예정)")
//                                    )
//                                    .build())
//                    ));
//        }
//
//        @Test
//        @MockMember(id = 1L)
//        @DisplayName("수정: 이름과 구성을 수정한다")
//        void update_portfolio() throws Exception {
//            // [Given] 필수 종목 데이터 생성
//            stockRepository.save(org.stockwellness.domain.stock.Stock.of(
//                    "TSLA", "테슬라", "TSLA", org.stockwellness.domain.stock.MarketType.KOSPI, 500000L, "456", "Tesla Inc."
//            ));
//
//            // given
//            Portfolio portfolio = savePortfolio(1L, "수정전");
//            List<PortfolioItemRequest> newItems = List.of(
//                    PortfolioFixture.createItemRequest("TSLA", 8, AssetType.STOCK)
//            );
//            PortfolioUpdateRequest updateRequest = PortfolioFixture.createUpdateRequest("수정후", newItems);
//
//            // when
//            mockMvc.perform(put("/api/v1/portfolios/{portfolioId}", portfolio.getId())
//                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
//                            .with(csrf())
//                            .contentType(APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(updateRequest)))
//                    .andExpect(status().isNoContent())
//                    .andDo(document("portfolio-update",
//                            resource(ResourceSnippetParameters.builder()
//                                    .tag("Portfolio")
//                                    .summary("포트폴리오 수정")
//                                    .description("포트폴리오의 이름, 설명 및 구성 종목을 수정합니다.")
//                                    .pathParameters(
//                                            parameterWithName("portfolioId").description("수정할 포트폴리오 ID")
//                                    )
//                                    .requestHeaders(
//                                            headerWithName("Authorization").description("Bearer Access Token")
//                                    )
//                                    .requestSchema(Schema.schema("PortfolioUpdateRequest"))
//                                    .requestFields(
//                                            fieldWithPath("name").description("수정할 이름"),
//                                            fieldWithPath("description").description("수정할 설명"),
//                                            fieldWithPath("items[].stockCode").description("종목 코드"),
//                                            fieldWithPath("items[].pieceCount").description("조각 수"),
//                                            fieldWithPath("items[].assetType").description("자산 타입")
//                                    )
//                                    .build())
//                    ));
//
//            // then
//            Portfolio updated = portfolioRepository.findById(portfolio.getId()).orElseThrow();
//            org.assertj.core.api.Assertions.assertThat(updated.getName()).isEqualTo("수정후");
//        }
//
//        @Test
//        @MockMember(id = 1L)
//        @DisplayName("삭제: 포트폴리오를 삭제한다")
//        void delete_portfolio() throws Exception {
//            // given
//            Portfolio portfolio = savePortfolio(1L, "삭제할 포트폴리오");
//
//            // when
//            mockMvc.perform(delete("/api/v1/portfolios/{portfolioId}", portfolio.getId())
//                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
//                            .with(csrf()))
//                    .andExpect(status().isNoContent())
//                    .andDo(document("portfolio-delete",
//                            resource(ResourceSnippetParameters.builder()
//                                    .tag("Portfolio")
//                                    .summary("포트폴리오 삭제")
//                                    .description("포트폴리오를 삭제합니다.")
//                                    .pathParameters(
//                                            parameterWithName("portfolioId").description("삭제할 포트폴리오 ID")
//                                    )
//                                    .requestHeaders(
//                                            headerWithName("Authorization").description("Bearer Access Token")
//                                    )
//                                    .build())
//                    ));
//
//            // then
//            org.assertj.core.api.Assertions.assertThat(portfolioRepository.findById(portfolio.getId())).isEmpty();
//        }
//
//        @Test
//        @MockMember(id = 1L)
//        @DisplayName("진단: 포트폴리오 건강 상태를 진단한다")
//        void diagnose_portfolio() throws Exception {
//            // [Given] 필수 종목 및 히스토리 데이터 생성
//            String isin = "KR7005930003";
//            stockRepository.save(org.stockwellness.domain.stock.Stock.of(
//                    isin, "삼성전자", "005930", org.stockwellness.domain.stock.MarketType.KOSPI, 1000000L, "123", "Samsung"
//            ));
//
//            StockPriceRepository.save(org.stockwellness.domain.stock.StockPrice.create(
//                    isin, java.time.LocalDate.now(),
//                    new java.math.BigDecimal("70000"), new java.math.BigDecimal("70000"),
//                    new java.math.BigDecimal("71000"), new java.math.BigDecimal("69000"),
//                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
//                    1000000L, new java.math.BigDecimal("70000000000"),
//                    new java.math.BigDecimal("400000000000000") // 400T
//            ));
//
//            Portfolio portfolio = savePortfolio(1L, "진단용 포트폴리오");
//            portfolio.updateItems(List.of(PortfolioItem.createStock(isin, 8)));
//            portfolioRepository.save(portfolio);
//
//            given(loadPortfolioAiPort.generatePortfolioInsight(any()))
//                    .willReturn(new PortfolioAiResult(
//                            "안정적인 대들보",
//                            "시가총액이 큰 우량주 중심의 포트폴리오로 매우 안정적입니다.",
//                            List.of("현재 비중을 유지하세요.", "현금 비중을 조금 늘려보세요.")
//                    ));
//
//            // when
//            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/health", portfolio.getId())
//                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
//                            .contentType(APPLICATION_JSON))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.overallScore").exists())
//                    .andExpect(jsonPath("$.summary", is("안정적인 대들보")))
//                    .andDo(document("portfolio-health",
//                            resource(ResourceSnippetParameters.builder()
//                                    .tag("Portfolio")
//                                    .summary("포트폴리오 건강 진단")
//                                    .description("포트폴리오의 수치적 건강 점수와 AI 인사이트를 조회합니다.")
//                                    .pathParameters(
//                                            parameterWithName("portfolioId").description("진단할 포트폴리오 ID")
//                                    )
//                                    .requestHeaders(
//                                            headerWithName("Authorization").description("Bearer Access Token")
//                                    )
//                                    .responseSchema(Schema.schema("DiagnosisResponse"))
//                                    .responseFields(
//                                            fieldWithPath("overallScore").description("종합 건강 점수 (0-100)"),
//                                            fieldWithPath("categories").description("카테고리별 점수 맵"),
//                                            fieldWithPath("categories.defense").description("방어력 점수"),
//                                            fieldWithPath("categories.attack").description("공격력 점수"),
//                                            fieldWithPath("categories.endurance").description("지구력 점수"),
//                                            fieldWithPath("categories.agility").description("민첩성 점수"),
//                                            fieldWithPath("categories.balance").description("균형성 점수"),
//                                            fieldWithPath("stockContributions").description("종목별 기여도 (준비 중)"),
//                                            fieldWithPath("summary").description("AI 요약 한줄평 (예: 성장하는 궁수)"),
//                                            fieldWithPath("insight").description("AI 상세 분석 내용"),
//                                            fieldWithPath("nextSteps").description("AI 추천 개선 단계 리스트")
//                                    )
//                                    .build())
//                    ));
//        }
//    }
//
//    @Nested
//    @DisplayName("실패 케이스")
//    class Failure {
//
//        @Test
//        @MockMember(id = 1L)
//        @DisplayName("검증: 총 조각 수가 8개를 초과하면 400 에러")
//        void fail_too_many_pieces() throws Exception {
//            // [Given] 필수 종목 데이터 생성
//            stockRepository.save(org.stockwellness.domain.stock.Stock.of(
//                    "AAPL", "애플", "AAPL", org.stockwellness.domain.stock.MarketType.KOSPI, 1000000L, "123", "Apple Inc."
//            ));
//            stockRepository.save(org.stockwellness.domain.stock.Stock.of(
//                    "MSFT", "마이크로소프트", "MSFT", org.stockwellness.domain.stock.MarketType.KOSPI, 800000L, "789", "Microsoft"
//            ));
//
//            List<PortfolioItemRequest> items = List.of(
//                    PortfolioFixture.createItemRequest("AAPL", 5, AssetType.STOCK),
//                    PortfolioFixture.createItemRequest("MSFT", 4, AssetType.STOCK)
//            );
//            PortfolioCreateRequest request = PortfolioFixture.createCreateRequest(items);
//
//            mockMvc.perform(post("/api/v1/portfolios")
//                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
//                            .with(csrf())
//                            .contentType(APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @MockMember(id = 1L)
//        @DisplayName("보안: 타인의 포트폴리오 수정 시도 시 401 Unauthorized")
//        void fail_update_others_portfolio() throws Exception {
//            // [Given] 다른 사용자의 포트폴리오 저장 (이름 중복 방지를 위해 유니크한 이름 사용)
//            Portfolio otherPortfolio = savePortfolio(OTHER_ID, "남의꺼_" + System.currentTimeMillis());
//
//            // [Given] 유효한 아이템 구성
//            List<PortfolioItemRequest> items = List.of(
//                    PortfolioFixture.createItemRequest("KRW", 1, AssetType.CASH)
//            );
//            PortfolioUpdateRequest request = PortfolioFixture.createUpdateRequest("탈취시도", items);
//
//            mockMvc.perform(put("/api/v1/portfolios/{portfolioId}", otherPortfolio.getId())
//                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
//                            .with(csrf())
//                            .contentType(APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
//                    .andExpect(status().isUnauthorized());
//        }
//    }
//
//    Portfolio savePortfolio(Long memberId, String name) {
//        Portfolio p = Portfolio.create(memberId, name, "설명");
//        p.updateItems(List.of(PortfolioItem.createCash(8)));
//        return portfolioRepository.save(p);
//    }
//}
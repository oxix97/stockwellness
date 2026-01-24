package org.stockwellness.adapter.in.web.portfolio;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioItemRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioUpdateRequest;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.fixture.PortfolioFixture;
import org.stockwellness.support.RestDocsSupport;

import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.is;
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

    static final String MY_ID = "1";
    static final String OTHER_ID = "99";

    // setUp()은 부모 클래스(RestDocsSupport)에서 처리됨

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("생성 및 조회: 포트폴리오를 생성하고 상세 조회한다")
        void create_and_get_portfolio() throws Exception {
            // [Given] 필수 종목 데이터 생성
            stockRepository.save(org.stockwellness.domain.stock.Stock.create(
                    "AAPL", "애플", "AAPL", org.stockwellness.domain.stock.MarketType.KOSPI, 1000000L, "123", "Apple Inc."
            ));
            stockRepository.save(org.stockwellness.domain.stock.Stock.create(
                    "TSLA", "테슬라", "TSLA", org.stockwellness.domain.stock.MarketType.KOSPI, 500000L, "456", "Tesla Inc."
            ));
            stockRepository.save(org.stockwellness.domain.stock.Stock.create(
                    "KRW", "원화", "KRW", org.stockwellness.domain.stock.MarketType.ETC, 0L, "000", "Korea Mint"
            ));

            // given
            List<PortfolioItemRequest> items = List.of(
                    PortfolioFixture.createItemRequest("AAPL", 4, AssetType.STOCK),
                    PortfolioFixture.createItemRequest("KRW", 4, AssetType.CASH)
            );
            PortfolioCreateRequest request = PortfolioFixture.createCreateRequest(items);

            // when (Create)
            mockMvc.perform(post("/api/v1/portfolios")
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andDo(document("portfolio-create",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("포트폴리오 생성")
                                    .description("새로운 투자 포트폴리오를 생성합니다.")
                                    .requestSchema(Schema.schema("PortfolioCreateRequest"))
                                    .requestHeaders(
                                            headerWithName("Authorization").description("Bearer Access Token")
                                    )
                                    .requestFields(
                                            fieldWithPath("name").description("포트폴리오 이름"),
                                            fieldWithPath("description").description("설명"),
                                            fieldWithPath("items[].stockCode").description("종목 코드 (ISIN)"),
                                            fieldWithPath("items[].pieceCount").description("보유 조각 수"),
                                            fieldWithPath("items[].assetType").description("자산 타입 (STOCK/CASH)")
                                    )
                                    .build())
                    ));

            Portfolio saved = portfolioRepository.findAll().stream()
                    .filter(p -> p.getName().equals(PortfolioFixture.NAME))
                    .findFirst().orElseThrow();

            // when (Get)
            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}", saved.getId())
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is(PortfolioFixture.NAME)))
                    .andDo(document("portfolio-get",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("포트폴리오 상세 조회")
                                    .pathParameters(
                                            parameterWithName("portfolioId").description("조회할 포트폴리오 ID")
                                    )
                                    .requestHeaders(
                                            headerWithName("Authorization").description("Bearer Access Token")
                                    )
                                    .responseSchema(Schema.schema("PortfolioResponse"))
                                    .responseFields(
                                            fieldWithPath("id").description("포트폴리오 ID"),
                                            fieldWithPath("name").description("포트폴리오 이름"),
                                            fieldWithPath("description").description("설명"),
                                            fieldWithPath("totalPieces").description("총 조각 수"),
                                            fieldWithPath("items[].stockCode").description("종목 코드"),
                                            fieldWithPath("items[].pieceCount").description("조각 수"),
                                            fieldWithPath("items[].assetType").description("자산 타입"),
                                            fieldWithPath("items[].piece").description("조각 (Deprecated 예정)")
                                    )
                                    .build())
                    ));
        }

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("수정: 이름과 구성을 수정한다")
        void update_portfolio() throws Exception {
            // [Given] 필수 종목 데이터 생성
            stockRepository.save(org.stockwellness.domain.stock.Stock.create(
                    "TSLA", "테슬라", "TSLA", org.stockwellness.domain.stock.MarketType.KOSPI, 500000L, "456", "Tesla Inc."
            ));
            
            // given
            Portfolio portfolio = savePortfolio(1L, "수정전");
            List<PortfolioItemRequest> newItems = List.of(
                    PortfolioFixture.createItemRequest("TSLA", 8, AssetType.STOCK)
            );
            PortfolioUpdateRequest updateRequest = PortfolioFixture.createUpdateRequest("수정후", newItems);

            // when
            mockMvc.perform(put("/api/v1/portfolios/{portfolioId}", portfolio.getId())
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNoContent())
                    .andDo(document("portfolio-update",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("포트폴리오 수정")
                                    .description("포트폴리오의 이름, 설명 및 구성 종목을 수정합니다.")
                                    .pathParameters(
                                            parameterWithName("portfolioId").description("수정할 포트폴리오 ID")
                                    )
                                    .requestHeaders(
                                            headerWithName("Authorization").description("Bearer Access Token")
                                    )
                                    .requestSchema(Schema.schema("PortfolioUpdateRequest"))
                                    .requestFields(
                                            fieldWithPath("name").description("수정할 이름"),
                                            fieldWithPath("description").description("수정할 설명"),
                                            fieldWithPath("items[].stockCode").description("종목 코드"),
                                            fieldWithPath("items[].pieceCount").description("조각 수"),
                                            fieldWithPath("items[].assetType").description("자산 타입")
                                    )
                                    .build())
                    ));

            // then
            Portfolio updated = portfolioRepository.findById(portfolio.getId()).orElseThrow();
            org.assertj.core.api.Assertions.assertThat(updated.getName()).isEqualTo("수정후");
        }

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("삭제: 포트폴리오를 삭제한다")
        void delete_portfolio() throws Exception {
            // given
            Portfolio portfolio = savePortfolio(1L, "삭제할 포트폴리오");

            // when
            mockMvc.perform(delete("/api/v1/portfolios/{portfolioId}", portfolio.getId())
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .with(csrf()))
                    .andExpect(status().isNoContent())
                    .andDo(document("portfolio-delete",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Portfolio")
                                    .summary("포트폴리오 삭제")
                                    .description("포트폴리오를 삭제합니다.")
                                    .pathParameters(
                                            parameterWithName("portfolioId").description("삭제할 포트폴리오 ID")
                                    )
                                    .requestHeaders(
                                            headerWithName("Authorization").description("Bearer Access Token")
                                    )
                                    .build())
                    ));

            // then
            org.assertj.core.api.Assertions.assertThat(portfolioRepository.findById(portfolio.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("검증: 총 조각 수가 8개를 초과하면 400 에러")
        void fail_too_many_pieces() throws Exception {
            // [Given] 필수 종목 데이터 생성
            stockRepository.save(org.stockwellness.domain.stock.Stock.create(
                    "AAPL", "애플", "AAPL", org.stockwellness.domain.stock.MarketType.KOSPI, 1000000L, "123", "Apple Inc."
            ));
            stockRepository.save(org.stockwellness.domain.stock.Stock.create(
                    "MSFT", "마이크로소프트", "MSFT", org.stockwellness.domain.stock.MarketType.KOSPI, 800000L, "789", "Microsoft"
            ));

            List<PortfolioItemRequest> items = List.of(
                    PortfolioFixture.createItemRequest("AAPL", 5, AssetType.STOCK),
                    PortfolioFixture.createItemRequest("MSFT", 4, AssetType.STOCK)
            );
            PortfolioCreateRequest request = PortfolioFixture.createCreateRequest(items);

            mockMvc.perform(post("/api/v1/portfolios")
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = MY_ID)
        @DisplayName("보안: 타인의 포트폴리오 수정 시도 시 403 Forbidden")
        void fail_update_others_portfolio() throws Exception {
            Portfolio otherPortfolio = savePortfolio(Long.parseLong(OTHER_ID), "남의꺼");
            PortfolioUpdateRequest request = PortfolioFixture.createUpdateRequest("탈취시도", List.of());

            mockMvc.perform(put("/api/v1/portfolios/{portfolioId}", otherPortfolio.getId())
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    Portfolio savePortfolio(Long memberId, String name) {
        Portfolio p = Portfolio.create(memberId, name, "설명");
        p.updateItems(List.of(PortfolioItem.createCash(8)));
        return portfolioRepository.save(p);
    }
}
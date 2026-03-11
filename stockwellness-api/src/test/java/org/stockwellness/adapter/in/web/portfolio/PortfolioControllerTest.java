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
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAiResult;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
import org.stockwellness.application.port.out.stock.LlmClientPort;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.*;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.application.service.portfolio.PortfolioFacade;
import org.stockwellness.fixture.PortfolioFixture;
import org.stockwellness.support.RestDocsSupport;
import org.stockwellness.support.annotation.MockMember;

import java.math.BigDecimal;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.is;
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
    PortfolioFacade portfolioFacade; // 실제 서비스 대신 파사드를 모킹

    @MockitoBean
    LoadPortfolioAiPort loadPortfolioAiPort;

    @MockitoBean
    LlmClientPort llmClientPort;

    static final Long MY_ID = 1L;
    static final Long OTHER_ID = 99L;

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
            mockMvc.perform(get("/api/v1/portfolios/{portfolioId}", 100L)
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    Portfolio savePortfolio(Long memberId, String name) {
        Portfolio p = Portfolio.create(memberId, name, "설명");
        p.updateItems(List.of(PortfolioItem.createCash(BigDecimal.valueOf(100), "KRW", BigDecimal.valueOf(100))));
        return portfolioRepository.save(p);
    }
}

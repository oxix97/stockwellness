package org.stockwellness.integration.stock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.fixture.StockFixture;
import org.stockwellness.integration.common.BaseIntegrationTest;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Stock API E2E 통합 테스트")
class StockIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private StockRepository stockRepository;

    private Stock savedStock;
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        stockRepository.deleteAll();
        savedStock = stockRepository.save(StockFixture.createSamsung());
        accessToken = loginAndGetToken("test@example.com", "tester");
    }

    @Test
    @DisplayName("종목 검색 API가 정상적으로 동작한다")
    void search_stocks_success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/stocks/search")
                        .param("keyword", "삼성")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].ticker").value("005930"))
                .andExpect(jsonPath("$.data.content[0].name").value("삼성전자"));
    }

    @Test
    @DisplayName("종목 상세 조회 API가 정상적으로 동작한다")
    void get_stock_detail_success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/stocks/{ticker}", "005930")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("005930"))
                .andExpect(jsonPath("$.data.name").value("삼성전자"));
    }
}

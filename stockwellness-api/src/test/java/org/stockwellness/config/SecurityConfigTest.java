package org.stockwellness.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.stockwellness.application.port.in.stock.StockPriceUseCase;
import org.stockwellness.application.port.in.stock.StockSearchUseCase;
import org.stockwellness.application.port.in.stock.StockUseCase;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security 인가 설정 테스트")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockUseCase stockUseCase;

    @MockitoBean
    private StockPriceUseCase stockPriceUseCase;

    @MockitoBean
    private StockSearchUseCase stockSearchUseCase;

    @Test
    @DisplayName("비로그인 상태에서 포트폴리오 접근 시 A008 코드를 반환한다")
    void portfolios_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A008"));
    }

    @Test
    @DisplayName("비로그인 상태에서 관심 그룹 접근 시 A008 코드를 반환한다")
    void watchlist_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/watchlist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A008"));
    }

    @Test
    @DisplayName("비로그인 상태에서 회원 정보 접근 시 A008 코드를 반환한다")
    void members_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A008"));
    }

    @Test
    @DisplayName("비로그인 상태에서 종목 상세 접근 시 200을 반환한다")
    void stocks_permitted() throws Exception {
        // given
        given(stockUseCase.getStockDetail(anyString()))
                .willReturn(null);

        // when & then
        mockMvc.perform(get("/api/v1/stocks/AAPL"))
                .andExpect(status().isOk());
    }
}

package org.stockwellness.adapter.out.persistence.stock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.stock.LoadStockHistoryPort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.CrossoverSignal;
import org.stockwellness.domain.stock.analysis.MarketCondition;
import org.stockwellness.domain.stock.analysis.TrendStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StockDataAdapterTest {

    @Autowired
    private StockDataAdapter stockDataAdapter;

    @MockitoBean
    private StockHistoryAdapter stockHistoryAdapter;

    @MockitoBean
    private StockTechnicalDataAdapter stockTechnicalDataAdapter;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        var cache = cacheManager.getCache("stock_info");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("주식 상세 정보를 조회하고 캐싱한다")
    void getSingleStockDetailCaching() {
        // given
        String isinCode = "KR7005930003";
        StockHistory history = StockHistory.create(isinCode, LocalDate.now(),
                BigDecimal.valueOf(70000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(72000), BigDecimal.valueOf(69000),
                BigDecimal.valueOf(-1000), BigDecimal.valueOf(-1.41),
                1000000L, BigDecimal.valueOf(70000000000L), BigDecimal.valueOf(400000000000000L));
        history.updateRsi14(BigDecimal.valueOf(45));

        given(stockHistoryAdapter.findLatestHistory(isinCode)).willReturn(Optional.of(history));

        // Mock LoadTechnicalDataPort
        MarketCondition condition = new MarketCondition(
                TrendStatus.REGULAR,
                CrossoverSignal.NONE,
                "Test description"
        );
        given(stockTechnicalDataAdapter.loadTechnicalContext(isinCode))
                .willReturn(AiAnalysisContext.of(history, condition));

        // when
        stockDataAdapter.getSingleStockDetail(isinCode);
        stockDataAdapter.getSingleStockDetail(isinCode); // Second call should hit cache

        // then
        verify(stockHistoryAdapter, times(1)).findLatestHistory(isinCode);
        assertThat(cacheManager.getCache("stock_info").get(isinCode)).isNotNull();
    }

    @Test
    @DisplayName("여러 주식의 상세 정보를 배치로 조회한다 (N+1 문제 해결)")
    void getStockDetailsBatch() {
        // given
        String isin1 = "S1";
        String isin2 = "S2";
        List<String> isinCodes = List.of(isin1, isin2);

        StockHistory h1 = createHistory(isin1);
        StockHistory h2 = createHistory(isin2);

        given(stockHistoryAdapter.loadRecentHistoriesBatch(isinCodes, 1))
                .willReturn(Map.of(isin1, List.of(h1), isin2, List.of(h2)));

        MarketCondition condition = new MarketCondition(TrendStatus.REGULAR, CrossoverSignal.NONE, "Desc");
        given(stockTechnicalDataAdapter.loadTechnicalContexts(isinCodes))
                .willReturn(Map.of(
                        isin1, AiAnalysisContext.of(h1, condition),
                        isin2, AiAnalysisContext.of(h2, condition)
                ));

        // when
        var results = stockDataAdapter.getStockDetails(isinCodes);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(isin1).isinCode()).isEqualTo(isin1);
        assertThat(results.get(isin2).isinCode()).isEqualTo(isin2);
        verify(stockHistoryAdapter, times(1)).loadRecentHistoriesBatch(isinCodes, 1);
        verify(stockTechnicalDataAdapter, times(1)).loadTechnicalContexts(isinCodes);
    }

    private StockHistory createHistory(String isinCode) {
        StockHistory history = StockHistory.create(isinCode, LocalDate.now(),
                BigDecimal.valueOf(10000), BigDecimal.valueOf(10000),
                BigDecimal.valueOf(10000), BigDecimal.valueOf(10000),
                BigDecimal.ZERO, BigDecimal.ZERO, 100L, BigDecimal.ZERO, BigDecimal.ZERO);
        history.updateRsi14(BigDecimal.valueOf(50));
        return history;
    }
}
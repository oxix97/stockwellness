package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.domain.stock.*;
import org.stockwellness.domain.stock.insight.LeadingStock;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SectorAnalysisServiceTest {

    private final SectorAnalysisService sectorAnalysisService = new SectorAnalysisService();

    @Test
    @DisplayName("섹터 지표 계산 및 연속 매수 일수 증가 검증")
    void analyzeIndicatorsAndConsecutiveDays() {
        // given
        MarketIndex index = new MarketIndex("001", "전기전자");
        LocalDate today = LocalDate.now();
        
        SectorApiDto currentData = new SectorApiDto(
                "001", "전기전자", today, new BigDecimal("2500.00"),
                new BigDecimal("1.50"), 1000L, 500L
        );
        
        // 전일 연속 매수 2일이었던 데이터
        SectorInsight yesterday = mock(SectorInsight.class);
        when(yesterday.getForeignConsecutiveBuyDays()).thenReturn(2);
        when(yesterday.getInstConsecutiveBuyDays()).thenReturn(1);

        // when
        SectorInsight result = sectorAnalysisService.analyze(index, currentData, yesterday, List.of(new BigDecimal("2400.00")), List.of());

        // then
        assertThat(result.getForeignConsecutiveBuyDays()).isEqualTo(3); // 2 + 1
        assertThat(result.getInstConsecutiveBuyDays()).isEqualTo(2); // 1 + 1
    }

    @Test
    @DisplayName("상승 종목 우선 주도주 추출 로직 검증")
    void calculateLeadingStocksWithGainers() {
        // given
        MarketIndex index = new MarketIndex("001", "전기전자");
        
        Stock stock1 = createStock("T001", "상승주1");
        Stock stock2 = createStock("T002", "하락주1");
        Stock stock3 = createStock("T003", "상승주2");

        StockPrice price1 = createPrice(stock1, new BigDecimal("2.0"), 10000L); // 상승
        StockPrice price2 = createPrice(stock2, new BigDecimal("-1.0"), 50000L); // 하락하지만 거래대금 높음
        StockPrice price3 = createPrice(stock3, new BigDecimal("3.0"), 5000L); // 상승

        List<StockPrice> stockPrices = List.of(price1, price2, price3);

        // when
        SectorInsight result = sectorAnalysisService.analyze(
                index, createDefaultApiDto(), null, List.of(), stockPrices
        );

        // then
        List<LeadingStock> leadingStocks = result.getLeadingStocks();
        // 상승주가 먼저 오고 거래대금 순으로 정렬되어야 함
        assertThat(leadingStocks).hasSize(2);
        assertThat(leadingStocks.get(0).ticker()).isEqualTo("T001"); // 거래대금 10000
        assertThat(leadingStocks.get(1).ticker()).isEqualTo("T003"); // 거래대금 5000
    }

    private SectorApiDto createDefaultApiDto() {
        return new SectorApiDto("001", "전기전자", LocalDate.now(), BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);
    }

    private Stock createStock(String ticker, String name) {
        return Stock.of(ticker, "KR001", name, MarketType.KOSPI, Currency.KRW, StockSector.of("IT", "IT", "001", "IT"), StockStatus.ACTIVE);
    }

    private StockPrice createPrice(Stock stock, BigDecimal rate, Long amt) {
        StockPrice price = mock(StockPrice.class);
        when(price.getStock()).thenReturn(stock);
        when(price.getFluctuationRate()).thenReturn(rate);
        when(price.getTransactionAmt()).thenReturn(BigDecimal.valueOf(amt));
        when(price.getClosePrice()).thenReturn(BigDecimal.valueOf(10000));
        return price;
    }
}

package org.stockwellness.batch.job.stock.price;

import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.domain.stock.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StockPriceTestFixture {

    public static Stock createSamsungStock() {
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", 
                MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 1L);
        return stock;
    }

    public static Stock createTestStock(Long id, String ticker, String name) {
        Stock stock = Stock.of(ticker, "KRTEST" + ticker, name, 
                MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", id);
        return stock;
    }

    /**
     * KisDailyPriceDetail 13개 필드 생성 헬퍼
     */
    public static KisDailyPriceDetail createDetail(LocalDate date, int price) {
        return new KisDailyPriceDetail(
                date, 
                BigDecimal.valueOf(price), 
                BigDecimal.valueOf(price + 100), 
                BigDecimal.valueOf(price - 100), 
                BigDecimal.valueOf(price), 
                100000L, 
                BigDecimal.valueOf(1000000000L), 
                "00", "0.00", "N", "3", BigDecimal.ZERO, ""
        );
    }

    public static KisMultiStockPriceDetail createMultiDetail(String ticker, String name, String price, String prevClose) {
        return new KisMultiStockPriceDetail(
                ticker, name, price, "0", "0.00", price, price, price, "100000", "1000000000", prevClose);
    }
}

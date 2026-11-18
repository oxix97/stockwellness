package org.stockwellness.domain.stock.price;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.stock.Stock;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockPrice {

    private StockPriceId id;
    private Stock stock;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal adjClosePrice;
    private BigDecimal previousClosePrice;
    private Long volume;
    private BigDecimal transactionAmt;
    private TechnicalIndicators indicators;

    /** 등락률 계산은 같은 가격 시리즈의 직전 종가가 있을 때만 수행합니다. */
    public BigDecimal getFluctuationRate() {
        if (closePrice == null || previousClosePrice == null || previousClosePrice.signum() <= 0) return null;
        return closePrice.subtract(previousClosePrice)
                .divide(previousClosePrice, new MathContext(16))
                .multiply(BigDecimal.valueOf(100));
    }

    public static StockPrice of(
            Stock stock,
            LocalDate baseDate,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal adjClose,
            BigDecimal previousClose,
            Long volume,
            BigDecimal transactionAmt,
            TechnicalIndicators indicators
    ) {
        var price = new StockPrice();
        price.id = new StockPriceId(baseDate, stock.getId());
        price.stock = stock;
        price.openPrice = open;
        price.highPrice = high;
        price.lowPrice = low;
        price.closePrice = close;
        price.adjClosePrice = adjClose;
        price.previousClosePrice = previousClose;
        price.volume = volume;
        price.transactionAmt = transactionAmt;
        price.indicators = indicators;
        return price;
    }
}

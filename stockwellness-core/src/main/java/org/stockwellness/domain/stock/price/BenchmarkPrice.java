package org.stockwellness.domain.stock.price;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
public class BenchmarkPrice {
    private String name;
    private String ticker;
    private LocalDate baseDate;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal changeRate;
    private Long volume;

    public static BenchmarkPrice of(String name, String ticker, LocalDate baseDate, BigDecimal closePrice) {
        BenchmarkPrice price = new BenchmarkPrice();
        price.name = name;
        price.ticker = ticker;
        price.baseDate = baseDate;
        price.closePrice = closePrice;
        return price;
    }

    public void updatePrices(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                             BigDecimal rate, Long volume) {
        this.openPrice = open;
        this.highPrice = high;
        this.lowPrice = low;
        this.closePrice = close;
        this.changeRate = rate;
        this.volume = volume;
    }
}

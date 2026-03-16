package org.stockwellness.domain;

import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 도메인 엔티티의 정적 팩토리 메서드를 활용한 테스트 데이터 생성 도구.
 * @Builder를 사용하지 않고 비즈니스 메서드를 통해 객체 생성을 캡슐화함.
 */
public class TestEntityFactory {

    public static Portfolio createPortfolio(Long id, String name) {
        Portfolio portfolio = Portfolio.create(1L, name, "Description");
        ReflectionTestUtils.setField(portfolio, "id", id);
        return portfolio;
    }

    public static Stock createStock(String ticker, String name, MarketType marketType) {
        return Stock.of(ticker, "ISIN-" + ticker, name, marketType, Currency.KRW, null, StockStatus.ACTIVE);
    }

    public static PortfolioItem createStockItem(String symbol, BigDecimal quantity, BigDecimal purchasePrice) {
        return PortfolioItem.createStock(symbol, quantity, purchasePrice, "KRW");
    }

    public static PortfolioItem createCashItem(BigDecimal amount) {
        return PortfolioItem.createCash(amount, "KRW");
    }

    public static StockPrice createStockPrice(Stock stock, LocalDate date, BigDecimal closePrice) {
        return StockPrice.of(stock, date, closePrice, closePrice, closePrice, closePrice, closePrice, closePrice, 1000L, BigDecimal.valueOf(1000000), null);
    }
}

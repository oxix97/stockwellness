package org.stockwellness.fixture;


import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

public class StockFixture {

    /**
     * 삼성전자 (KOSPI, KRW, 일반주식)
     */
    public static Stock createSamsung() {
        return Stock.of(
                "005930",
                "KR7005930003",
                "삼성전자",
                MarketType.KOSPI,
                Currency.KRW,
                "009",
                "전기전자",
                StockStatus.ACTIVE
        );
    }

    /**
     * 애플 (NASDAQ, USD, 해외주식)
     */
    public static Stock createApple() {
        return Stock.of(
                "AAPL",
                "US0378331005",
                "Apple Inc",
                MarketType.NASDAQ,
                Currency.USD,
                "TEC",
                "Technology",
                StockStatus.ACTIVE
        );
    }

    /**
     * KODEX 200 (KOSPI, KRW, ETF)
     */
    public static Stock createKodex200() {
        return Stock.of(
                "069500",
                "KR7069500007",
                "KODEX 200",
                MarketType.KOSPI,
                Currency.KRW,
                "ETF",
                "상장지수펀드",
                StockStatus.ACTIVE
        );
    }

    /**
     * 상장 폐지된 종목 (테스트용)
     */
    public static Stock createDelisted(String ticker, String name) {
        return Stock.of(
                ticker,
                "DELISTED_STD_" + ticker,
                name,
                MarketType.KOSPI,
                Currency.KRW,
                "999",
                "미분류",
                StockStatus.DELISTED
        );
    }
}
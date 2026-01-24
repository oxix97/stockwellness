package org.stockwellness.fixture;

import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.domain.stock.StockStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StockFixture {

    public static final String ISIN_CODE = "KR7005930003";
    public static final String TICKER = "005930";
    public static final String NAME = "삼성전자";

    /**
     * 기본 Stock 엔티티 생성
     */
    public static Stock createStock() {
        return createStock(ISIN_CODE, TICKER, NAME);
    }

    public static Stock createStock(String isinCode, String ticker, String name) {
        return Stock.create(
                isinCode,
                name,
                ticker,
                MarketType.KOSPI,
                5969782550L,
                "123456789",
                "삼성전자주식회사"
        );
    }

    /**
     * 기본 StockHistory 생성 (필수값만 입력, 나머지는 더미 데이터)
     */
    public static StockHistory createHistory(String isinCode, LocalDate baseDate, double closePrice) {
        return StockHistory.create(
                isinCode,
                baseDate,
                BigDecimal.valueOf(closePrice),
                BigDecimal.valueOf(closePrice),
                BigDecimal.valueOf(closePrice),
                BigDecimal.valueOf(closePrice),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                100000L,
                BigDecimal.valueOf(1000000000),
                BigDecimal.valueOf(500000000000L)
        );
    }

    /**
     * 지표가 포함된 StockHistory 생성
     */
    public static StockHistory createHistoryWithIndicators(
            String isinCode, LocalDate baseDate, double closePrice,
            double ma5, double ma20, double rsi, double macd) {
        
        StockHistory history = createHistory(isinCode, baseDate, closePrice);
        
        history.updateMa5(BigDecimal.valueOf(ma5));
        history.updateMa20(BigDecimal.valueOf(ma20));
        history.updateRsi14(BigDecimal.valueOf(rsi));
        history.updateMacd(BigDecimal.valueOf(macd));
        
        return history;
    }

    /**
     * 검색 쿼리 생성
     */
    public static SearchStockQuery createSearchQuery(String keyword) {
        return new SearchStockQuery(keyword, MarketType.KOSPI, StockStatus.ACTIVE, 1, 20);
    }

    /**
     * 검색 결과 생성
     */
    public static StockSearchResult createSearchResult(String isinCode, String name) {
        return new StockSearchResult(isinCode, "TICKER", name, "KOSPI", 1000000L);
    }

    /**
     * 상세 결과 생성
     */
    public static StockDetailResult createDetailResult(String isinCode, String name) {
        return new StockDetailResult(
                isinCode, "TICKER", name, "KOSPI", null, 1000000L,
                LocalDate.now(), BigDecimal.valueOf(50000), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(51000), BigDecimal.valueOf(49000),
                100000L, BigDecimal.valueOf(5000000000L), BigDecimal.valueOf(300000000000000L),
                BigDecimal.valueOf(50), BigDecimal.valueOf(50000)
        );
    }
}

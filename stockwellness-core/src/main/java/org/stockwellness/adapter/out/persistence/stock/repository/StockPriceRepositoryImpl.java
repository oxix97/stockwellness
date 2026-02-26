package org.stockwellness.adapter.out.persistence.stock.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.stockwellness.domain.stock.QStock.stock;
import static org.stockwellness.domain.stock.price.QStockPrice.stockPrice;

@RequiredArgsConstructor
public class StockPriceRepositoryImpl implements StockPriceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, LocalDate> findLatestBaseDatesByStocks(List<Stock> stocks) {
        List<Tuple> results = queryFactory
                .select(stockPrice.stock.id, stockPrice.id.baseDate.max())
                .from(stockPrice)
                .where(stockPrice.stock.in(stocks))
                .groupBy(stockPrice.stock.id)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        t -> t.get(stockPrice.stock.id),
                        t -> t.get(stockPrice.id.baseDate.max())
                ));
    }

    @Override
    public List<StockPrice> findRecentPricesByStocks(List<Stock> stocks, LocalDate date) {
        return queryFactory
                .selectFrom(stockPrice)
                .join(stockPrice.stock, stock).fetchJoin()
                .where(
                        stockPrice.stock.in(stocks),
                        stockPrice.id.baseDate.lt(date)
                )
                .orderBy(stockPrice.id.baseDate.desc())
                .limit(stocks.size() * 130L)
                .fetch();
    }

    @Override
    public List<StockPrice> findByStockInAndIdBaseDate(List<Stock> stocks, LocalDate baseDate) {
        return queryFactory
                .selectFrom(stockPrice)
                .join(stockPrice.stock, stock).fetchJoin() // N+1 방지
                .where(
                        stock.in(stocks),
                        stockPrice.id.baseDate.eq(baseDate)
                )
                .fetch();
    }

    @Override
    public List<StockPriceResult> findAllByTickerAndYear(String ticker, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return findAllByTickerAndPeriod(ticker, start, end);
    }

    @Override
    public List<StockPriceResult> findAllByTickerAndPeriod(String ticker, LocalDate start, LocalDate end) {
        return queryFactory
                .select(Projections.constructor(StockPriceResult.class,
                        stockPrice.id.baseDate,
                        stockPrice.openPrice,
                        stockPrice.highPrice,
                        stockPrice.lowPrice,
                        stockPrice.closePrice,
                        stockPrice.adjClosePrice,
                        stockPrice.volume
                ))
                .from(stockPrice)
                .join(stockPrice.stock, stock)
                .where(
                        stock.ticker.eq(ticker),
                        stockPrice.id.baseDate.between(start, end)
                )
                .orderBy(stockPrice.id.baseDate.asc())
                .fetch();
    }
}

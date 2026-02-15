package org.stockwellness.adapter.out.persistence.stock.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.time.LocalDate;
import java.util.List;

import static org.stockwellness.domain.stock.QStock.stock;
import static org.stockwellness.domain.stock.QStockPrice.stockPrice;

@RequiredArgsConstructor
public class StockPriceRepositoryImpl implements StockPriceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

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

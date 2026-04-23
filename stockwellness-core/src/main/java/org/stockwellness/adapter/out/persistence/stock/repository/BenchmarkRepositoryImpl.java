package org.stockwellness.adapter.out.persistence.stock.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.time.LocalDate;
import java.util.List;

import static org.stockwellness.domain.stock.price.QBenchmarkPrice.benchmarkPrice;

@Repository
@RequiredArgsConstructor
public class BenchmarkRepositoryImpl implements BenchmarkRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<StockPriceResult> findBenchmarkPrices(String ticker, LocalDate start, LocalDate end) {
        return findBenchmarkPricesIn(List.of(ticker), start, end);
    }

    @Override
    public List<StockPriceResult> findBenchmarkPricesIn(List<String> tickers, LocalDate start, LocalDate end) {
        return queryFactory
                .select(Projections.constructor(StockPriceResult.class,
                        benchmarkPrice.baseDate,
                        benchmarkPrice.openPrice,
                        benchmarkPrice.highPrice,
                        benchmarkPrice.lowPrice,
                        benchmarkPrice.closePrice,
                        benchmarkPrice.closePrice.as("adjClosePrice"),
                        benchmarkPrice.volume,
                        benchmarkPrice.closePrice.multiply(benchmarkPrice.volume).as("transactionAmt"),
                        benchmarkPrice.closePrice.as("ma5"),
                        benchmarkPrice.closePrice.as("ma20"),
                        benchmarkPrice.closePrice.as("ma60"),
                        benchmarkPrice.closePrice.as("ma120"),
                        benchmarkPrice.changeRate,
                        benchmarkPrice.ticker // Ticker 정보도 포함하여 반환 (그룹화 용도)
                ))
                .from(benchmarkPrice)
                .where(
                        benchmarkPrice.ticker.in(tickers),
                        benchmarkPrice.baseDate.between(start, end)
                )
                .orderBy(benchmarkPrice.baseDate.asc())
                .fetch();
    }
}

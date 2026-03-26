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
        return queryFactory
                .select(Projections.constructor(StockPriceResult.class,
                        benchmarkPrice.baseDate,
                        benchmarkPrice.openPrice,
                        benchmarkPrice.highPrice,
                        benchmarkPrice.lowPrice,
                        benchmarkPrice.closePrice,
                        benchmarkPrice.closePrice.as("adjClosePrice"), // 지수는 수정종가 개념이 없으므로 종가 사용
                        benchmarkPrice.volume,
                        benchmarkPrice.closePrice.multiply(benchmarkPrice.volume).as("transactionAmt"), // 간이 계산
                        benchmarkPrice.closePrice.as("ma5"), // 필요 시 연산 추가 가능하나 현재는 기본값
                        benchmarkPrice.closePrice.as("ma20"),
                        benchmarkPrice.closePrice.as("ma60"),
                        benchmarkPrice.closePrice.as("ma120")
                ))
                .from(benchmarkPrice)
                .where(
                        benchmarkPrice.ticker.eq(ticker),
                        benchmarkPrice.baseDate.between(start, end)
                )
                .orderBy(benchmarkPrice.baseDate.asc())
                .fetch();
    }
}

package org.stockwellness.adapter.out.persistence.stock.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.stockwellness.domain.stock.StockHistory;

import java.time.LocalDate;
import java.util.List;

import static org.stockwellness.domain.stock.QStockHistory.stockHistory;

@RequiredArgsConstructor
public class StockHistoryRepositoryImpl implements StockHistoryCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<StockHistory> findRecentHistory(String isinCode, LocalDate targetDate, int limit) {
        return queryFactory
                .selectFrom(stockHistory)
                .where(
                        stockHistory.isinCode.eq(isinCode),
                        stockHistory.baseDate.loe(targetDate)
                )
                .orderBy(stockHistory.baseDate.desc())
                .limit(limit)
                .fetch();
    }
}

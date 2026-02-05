package org.stockwellness.adapter.out.persistence.stock.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.stockwellness.domain.stock.StockHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.stockwellness.domain.stock.QStockHistory.stockHistory;

@RequiredArgsConstructor
public class StockHistoryCustomRepositoryImpl implements StockHistoryCustomRepository {

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

    @Override
    public Map<String, List<StockHistory>> findRecentHistoryBatch(List<String> isinCodes, int limit) {
        List<StockHistory> all = queryFactory
                .selectFrom(stockHistory)
                .where(stockHistory.isinCode.in(isinCodes))
                .orderBy(stockHistory.baseDate.desc())
                .limit((long) isinCodes.size() * limit)
                .fetch();

        return all.stream()
                .collect(Collectors.groupingBy(StockHistory::getIsinCode))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().limit(limit).collect(Collectors.toList())
                ));
    }
}

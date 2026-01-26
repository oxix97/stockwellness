package org.stockwellness.adapter.out.persistence.stock.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.util.StringUtils;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

import java.util.List;

import static org.stockwellness.domain.stock.QStock.stock;

@RequiredArgsConstructor
public class StockCustomRepositoryImpl implements StockCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Stock> searchByCondition(
            String keyword,
            MarketType marketType,
            StockStatus status,
            Pageable pageable) {

        List<Stock> content = queryFactory
                .selectFrom(stock)
                .where(
                        keywordContains(keyword),
                        marketTypeEq(marketType),
                        statusEq(status)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return stock.name.containsIgnoreCase(keyword)
                .or(stock.ticker.containsIgnoreCase(keyword));
    }

    private BooleanExpression marketTypeEq(MarketType marketType) {
        return marketType != null ? stock.marketType.eq(marketType) : null;
    }

    private BooleanExpression statusEq(StockStatus status) {
        return status != null ? stock.status.eq(status) : null;
    }
}

package org.stockwellness.adapter.out.persistence.stock.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.util.StringUtils;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;

import java.util.Collections;
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

        if (!StringUtils.hasText(keyword)) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }

        StringPath name = stock.name;
        StringPath ticker = stock.ticker;

        List<Stock> content = queryFactory
                .selectFrom(stock)
                .where(
                        keywordContains(keyword, name, ticker),
                        marketTypeEq(marketType),
                        statusEq(status)
                )
                .orderBy(
                        rank(keyword, name, ticker).asc(),
                        name.asc()
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

    private NumberExpression<Integer> rank(String keyword, StringPath name, StringPath ticker) {
        return new CaseBuilder()
                .when(ticker.equalsIgnoreCase(keyword)).then(1)
                .when(name.equalsIgnoreCase(keyword)).then(2)
                .when(name.startsWithIgnoreCase(keyword)).then(3)
                .otherwise(4);
    }

    private BooleanExpression keywordContains(String keyword, StringPath name, StringPath ticker) {
        return name.containsIgnoreCase(keyword).or(ticker.containsIgnoreCase(keyword));
    }

    private BooleanExpression marketTypeEq(MarketType marketType) {
        return marketType != null ? stock.marketType.eq(marketType) : null;
    }

    private BooleanExpression statusEq(StockStatus status) {
        return status != null ? stock.status.eq(status) : null;
    }
}

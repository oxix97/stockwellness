package org.stockwellness.adapter.out.persistence.stock.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.AlignmentStatus;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TradeDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.stockwellness.domain.stock.QStock.stock;
import static org.stockwellness.domain.stock.price.QStockPrice.stockPrice;

@RequiredArgsConstructor
public class StockPriceRepositoryImpl implements StockPriceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<LocalDate> findLatestDate() {
        return Optional.ofNullable(
                queryFactory
                        .select(stockPrice.id.baseDate.max())
                        .from(stockPrice)
                        .fetchOne()
        );
    }

    @Override
    public Optional<LocalDate> findLatestDateOnOrBefore(LocalDate date) {
        return Optional.ofNullable(
                queryFactory
                        .select(stockPrice.id.baseDate.max())
                        .from(stockPrice)
                        .where(stockPrice.id.baseDate.loe(date))
                        .fetchOne()
        );
    }

    /**
     * 종목명(name)으로 해당 종목의 가장 최신 StockPrice 1건 조회
     */
    @Override
    public Optional<StockPrice> findLatestPriceByName(String name) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(stockPrice)
                        .join(stockPrice.stock, stock)
                        .where(stock.name.eq(name))
                        .orderBy(stockPrice.id.baseDate.desc())
                        .fetchFirst()
        );
    }

    @Override
    public List<StockSupplyRankingResult> findTopInstitutionStocksBySupply(
            LocalDate date,
            TradeDirection direction,
            int limit
    ) {
        return findTopStocksBySupply(
                date,
                stockPrice.netInstitutionalBuyingQty.coalesce(0L),
                stockPrice.netInstitutionalBuyingAmt.coalesce(BigDecimal.ZERO),
                direction,
                limit
        );
    }

    @Override
    public List<StockSupplyRankingResult> findTopForeignStocksBySupply(
            LocalDate date,
            TradeDirection direction,
            int limit
    ) {
        return findTopStocksBySupply(
                date,
                stockPrice.netForeignBuyingQty.coalesce(0L),
                stockPrice.netForeignBuyingAmt.coalesce(BigDecimal.ZERO),
                direction,
                limit
        );
    }

    private List<StockSupplyRankingResult> findTopStocksBySupply(
            LocalDate date,
            NumberExpression<Long> buyingQty,
            NumberExpression<BigDecimal> buyingAmt,
            TradeDirection direction,
            int limit
    ) {
        BooleanExpression directionFilter = getDirectionFilter(buyingQty, direction);
        NumberExpression<BigDecimal> currentPrice = stockPrice.closePrice.coalesce(BigDecimal.ZERO);
        NumberExpression<BigDecimal> fluctuationRate = new CaseBuilder()
                .when(stockPrice.previousClosePrice.isNull().or(stockPrice.previousClosePrice.eq(BigDecimal.ZERO)))
                .then(BigDecimal.ZERO)
                .otherwise(currentPrice.subtract(stockPrice.previousClosePrice)
                        .divide(stockPrice.previousClosePrice)
                        .multiply(BigDecimal.valueOf(100)));

        return queryFactory
                .select(Projections.constructor(StockSupplyRankingResult.class,
                        stock.ticker,
                        stock.name,
                        stock.sector.sectorName,
                        currentPrice,
                        fluctuationRate,
                        buyingQty,
                        buyingAmt,
                        stockPrice.transactionAmt
                ))
                .from(stockPrice)
                .join(stockPrice.stock, stock)
                .where(
                        stockPrice.id.baseDate.eq(date),
                        directionFilter
                )
                .orderBy(direction == TradeDirection.BUY ? buyingQty.desc() : buyingQty.asc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression getDirectionFilter(NumberExpression<Long> buyingQty, TradeDirection direction) {
        return direction == TradeDirection.BUY ? buyingQty.gt(0L) : buyingQty.lt(0L);
    }

    @Override
    public List<StockPrice> findFilteredStocksByIndicators(
            LocalDate baseDate,
            AlignmentStatus alignment,
            BigDecimal rsiLow,
            BigDecimal rsiHigh,
            Boolean isGoldenCross
    ) {
        return queryFactory
                .selectFrom(stockPrice)
                .join(stockPrice.stock, stock).fetchJoin()
                .where(
                        stockPrice.id.baseDate.eq(baseDate),
                        eqAlignment(alignment),
                        betweenRsi(rsiLow, rsiHigh),
                        eqGoldenCross(isGoldenCross)
                )
                .limit(100)
                .fetch();
    }

    private BooleanExpression eqAlignment(AlignmentStatus alignment) {
        return alignment != null ? stockPrice.indicators.alignmentStatus.eq(alignment) : null;
    }

    private BooleanExpression betweenRsi(BigDecimal rsiLow, BigDecimal rsiHigh) {
        if (rsiLow != null && rsiHigh != null) {
            return stockPrice.indicators.rsi14.between(rsiLow, rsiHigh);
        } else if (rsiLow != null) {
            return stockPrice.indicators.rsi14.goe(rsiLow);
        } else if (rsiHigh != null) {
            return stockPrice.indicators.rsi14.loe(rsiHigh);
        }
        return null;
    }

    private BooleanExpression eqGoldenCross(Boolean isGoldenCross) {
        return isGoldenCross != null ? stockPrice.indicators.isGoldenCross.eq(isGoldenCross) : null;
    }

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
    public List<StockPrice> findRecentPricesByStocks(List<Stock> stocks, LocalDate date, int limit) {
        // [수정] 30개 종목에 대해 각각 최근 limit(120일)치를 충분히 가져오도록 개선
        // 특정 종목의 데이터가 적을 수 있으므로 전체 조회 시 넉넉하게 limit * 1.5배를 가져오도록 함
        long totalLimit = (long) stocks.size() * limit;
        long safetyBuffer = (long) (totalLimit * 1.5);

        return queryFactory
                .selectFrom(stockPrice)
                .join(stockPrice.stock, stock).fetchJoin()
                .where(
                        stockPrice.stock.in(stocks),
                        stockPrice.id.baseDate.lt(date)
                )
                .orderBy(stockPrice.id.baseDate.desc())
                .limit(safetyBuffer)
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
    public List<StockPrice> findAllByDateWithStock(LocalDate baseDate) {
        return queryFactory
                .selectFrom(stockPrice)
                .join(stockPrice.stock, stock).fetchJoin()
                .where(stockPrice.id.baseDate.eq(baseDate))
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
                        stockPrice.volume,
                        stockPrice.transactionAmt,
                        stockPrice.indicators.ma5,
                        stockPrice.indicators.ma20,
                        stockPrice.indicators.ma60,
                        stockPrice.indicators.ma120
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

    @Override
    public Map<String, BigDecimal> findLatestPricesByTickers(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Map.of();
        }

        // 1. 각 티커별 가장 최신 시세 날짜를 먼저 조회
        List<Tuple> latestDates = queryFactory
                .select(stockPrice.stock.ticker, stockPrice.id.baseDate.max())
                .from(stockPrice)
                .where(stockPrice.stock.ticker.in(tickers))
                .groupBy(stockPrice.stock.ticker)
                .fetch();

        if (latestDates.isEmpty()) {
            return Map.of();
        }

        // 2. 티커와 최신 날짜 조합으로 필터링하기 위한 조건 생성 (OR 결합)
        BooleanExpression predicate = null;
        for (Tuple row : latestDates) {
            String t = row.get(stockPrice.stock.ticker);
            LocalDate d = row.get(stockPrice.id.baseDate.max());
            if (t != null && d != null) {
                BooleanExpression current = stockPrice.stock.ticker.eq(t).and(stockPrice.id.baseDate.eq(d));
                predicate = (predicate == null) ? current : predicate.or(current);
            }
        }

        if (predicate == null) return Map.of();

        // 3. 최종 시세(종가) 조회
        List<Tuple> results = queryFactory
                .select(stockPrice.stock.ticker, stockPrice.closePrice)
                .from(stockPrice)
                .where(predicate)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        t -> t.get(stockPrice.stock.ticker),
                        t -> t.get(stockPrice.closePrice) != null ? t.get(stockPrice.closePrice) : BigDecimal.ZERO,
                        (existing, replacement) -> existing // 중복 방지 (이론상 발생 안함)
                ));
    }
}

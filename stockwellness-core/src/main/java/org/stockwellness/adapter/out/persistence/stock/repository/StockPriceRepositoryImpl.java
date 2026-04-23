package org.stockwellness.adapter.out.persistence.stock.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;
import org.stockwellness.application.port.out.stock.MarketBreadthItem;
import org.stockwellness.application.port.out.stock.MarketBreadthSnapshot;
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
import static org.stockwellness.domain.stock.price.QStockInvestorTrade.stockInvestorTrade;
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

    @Override
    public Optional<LocalDate> findLatestInvestorTradeDate() {
        return Optional.ofNullable(
                queryFactory
                        .select(stockInvestorTrade.id.baseDate.max())
                        .from(stockInvestorTrade)
                        .join(stockPrice).on(
                                stockPrice.stock.eq(stockInvestorTrade.stock)
                                        .and(stockPrice.id.baseDate.eq(stockInvestorTrade.id.baseDate))
                        )
                        .fetchOne()
        );
    }

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
                stockInvestorTrade.orgnNtbyQty,
                stockInvestorTrade.orgnNtbyTrPbmn,
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
                stockInvestorTrade.frgnNtbyQty,
                stockInvestorTrade.frgnNtbyTrPbmn,
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
        NumberExpression<Long> netBuyingQuantity = buyingQty.coalesce(0L);
        NumberExpression<BigDecimal> netBuyingAmount = buyingAmt.coalesce(BigDecimal.ZERO);
        BooleanExpression directionFilter = getDirectionFilter(netBuyingAmount, direction);
        NumberExpression<BigDecimal> currentPrice = stockPrice.closePrice.coalesce(BigDecimal.ZERO);
        NumberExpression<BigDecimal> transactionAmount = stockPrice.transactionAmt.coalesce(BigDecimal.ZERO);
        NumberExpression<BigDecimal> fluctuationRate = new CaseBuilder()
                .when(stockPrice.previousClosePrice.isNull().or(stockPrice.previousClosePrice.eq(BigDecimal.ZERO)))
                .then(BigDecimal.ZERO)
                .otherwise(currentPrice.subtract(stockPrice.previousClosePrice)
                        .divide(stockPrice.previousClosePrice.nullif(BigDecimal.ZERO))
                        .multiply(BigDecimal.valueOf(100)));

        return queryFactory
                .select(Projections.constructor(StockSupplyRankingResult.class,
                        stock.ticker,
                        stock.name,
                        stock.sector.sectorName,
                        currentPrice,
                        fluctuationRate,
                        netBuyingQuantity,
                        netBuyingAmount,
                        transactionAmount
                ))
                .from(stockInvestorTrade)
                .join(stockInvestorTrade.stock, stock)
                .leftJoin(stockPrice).on(
                        stockPrice.stock.eq(stockInvestorTrade.stock)
                                .and(stockPrice.id.baseDate.eq(stockInvestorTrade.id.baseDate))
                )
                .where(
                        stockInvestorTrade.id.baseDate.eq(date),
                        directionFilter
                )
                .orderBy(direction == TradeDirection.BUY ? netBuyingAmount.desc() : netBuyingAmount.asc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression getDirectionFilter(NumberExpression<BigDecimal> buyingAmt, TradeDirection direction) {
        return direction == TradeDirection.BUY ? buyingAmt.gt(BigDecimal.ZERO) : buyingAmt.lt(BigDecimal.ZERO);
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
                .join(stockPrice.stock, stock).fetchJoin()
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
    public List<MarketBreadthItem> findAllBreadthItemsByDate(LocalDate baseDate) {
        return queryFactory
                .select(Projections.constructor(MarketBreadthItem.class,
                        stockPrice.id.baseDate,
                        stockPrice.openPrice,
                        stockPrice.highPrice,
                        stockPrice.lowPrice,
                        stockPrice.closePrice,
                        stockPrice.previousClosePrice
                ))
                .from(stockPrice)
                .where(stockPrice.id.baseDate.eq(baseDate))
                .fetch();
    }

    @Override
    public MarketBreadthSnapshot summarizeBreadthByDate(LocalDate baseDate) {
        NumberExpression<BigDecimal> base = Expressions.asNumber(
                new CaseBuilder()
                        .when(stockPrice.previousClosePrice.isNotNull().and(stockPrice.previousClosePrice.gt(BigDecimal.ZERO)))
                        .then(stockPrice.previousClosePrice)
                        .otherwise(stockPrice.openPrice)
        );

        NumberExpression<BigDecimal> fluctuationRate = stockPrice.closePrice.subtract(base)
                .divide(base.nullif(BigDecimal.ZERO))
                .multiply(BigDecimal.valueOf(100));

        NumberExpression<BigDecimal> intradaySwing = stockPrice.highPrice.subtract(stockPrice.lowPrice)
                .divide(base.nullif(BigDecimal.ZERO))
                .multiply(BigDecimal.valueOf(100));

        Tuple counts = queryFactory
                .select(
                        stockPrice.count(),
                        new CaseBuilder().when(fluctuationRate.gt(new BigDecimal("0.15"))).then(1).otherwise(0).sum(),
                        new CaseBuilder().when(fluctuationRate.lt(new BigDecimal("-0.15"))).then(1).otherwise(0).sum(),
                        new CaseBuilder().when(fluctuationRate.abs().loe(new BigDecimal("0.15"))).then(1).otherwise(0).sum(),
                        new CaseBuilder()
                                .when(fluctuationRate.abs().goe(new BigDecimal("3.00"))
                                        .or(intradaySwing.goe(new BigDecimal("4.00"))))
                                .then(1).otherwise(0).sum()
                )
                .from(stockPrice)
                .where(stockPrice.id.baseDate.eq(baseDate))
                .fetchOne();

        if (counts == null || counts.get(0, Long.class) == 0) {
            return null;
        }

        long total = counts.get(0, Long.class);
        int advancing = counts.get(1, Integer.class) != null ? counts.get(1, Integer.class) : 0;
        int declining = counts.get(2, Integer.class) != null ? counts.get(2, Integer.class) : 0;
        int unchanged = counts.get(3, Integer.class) != null ? counts.get(3, Integer.class) : 0;
        int highVolatility = counts.get(4, Integer.class) != null ? counts.get(4, Integer.class) : 0;

        BigDecimal totalDecimal = BigDecimal.valueOf(total);
        return new MarketBreadthSnapshot(
                (int) total,
                advancing,
                declining,
                unchanged,
                highVolatility,
                ratio(advancing, totalDecimal),
                ratio(declining, totalDecimal),
                ratio(highVolatility, totalDecimal)
        );
    }

    private BigDecimal ratio(int count, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count).divide(total, 4, java.math.RoundingMode.HALF_UP);
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
                        stockPrice.indicators.ma120,
                        Expressions.asNumber(BigDecimal.ZERO),
                        stock.ticker
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
    public Map<String, List<StockPrice>> findRecentPricesBatch(List<String> tickers, int limit) {
        if (tickers == null || tickers.isEmpty()) {
            return Map.of();
        }

        List<StockPrice> allPrices = queryFactory
                .selectFrom(stockPrice)
                .join(stockPrice.stock, stock).fetchJoin()
                .where(stock.ticker.in(tickers))
                .orderBy(stock.ticker.asc(), stockPrice.id.baseDate.desc())
                .fetch();

        return allPrices.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStock().getTicker(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream().limit(limit).toList()
                        )
                ));
    }

    @Override
    public Map<String, BigDecimal> findLatestPricesByTickers(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Map.of();
        }

        List<StockPrice> latests = queryFactory
                .selectFrom(stockPrice)
                .join(stockPrice.stock, stock).fetchJoin()
                .where(stock.ticker.in(tickers))
                .orderBy(stock.ticker.asc(), stockPrice.id.baseDate.desc())
                .fetch();

        return latests.stream()
                .collect(Collectors.toMap(
                        p -> p.getStock().getTicker(),
                        p -> p.getClosePrice() != null ? p.getClosePrice() : BigDecimal.ZERO,
                        (existing, replacement) -> existing
                ));
    }
}

package org.stockwellness.adapter.batch.stockprice.step.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.application.port.out.external.kis.KisDailyPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.global.util.DateUtil;

@RequiredArgsConstructor
public class DailyStockPriceProcessor implements ItemProcessor<List<Stock>, List<StockPrice>> {

    private final KisDailyPricePort kisDailyPricePort;
    private final LocalDate targetDate;

    @Nullable
    @Override
    public List<StockPrice> process(@NonNull List<Stock> stocks) {
        LocalDate today = DateUtil.today();
        if (targetDate.isAfter(today)) {
            throw new IllegalArgumentException("주식 시세 기준일은 오늘 이후일 수 없습니다: " + targetDate);
        }
        return targetDate.isBefore(today) ? processHistorical(stocks) : processToday(stocks);
    }

    private List<StockPrice> processToday(List<Stock> stocks) {
        Map<String, KisMultiStockPriceDetail> priceMap = kisDailyPricePort.fetchMultiStockPrices(
                        stocks.stream()
                                .map(Stock::getTicker)
                                .toList()
                ).stream()
                .collect(Collectors.toMap(
                        KisMultiStockPriceDetail::ticker,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        List<String> missingTickers = stocks.stream()
                .map(Stock::getTicker)
                .filter(ticker -> !priceMap.containsKey(ticker))
                .distinct()
                .toList();
        if (!missingTickers.isEmpty()) {
            throw new IllegalStateException("KIS 멀티시세 응답에서 종목이 누락되었습니다: " + missingTickers);
        }

        return stocks.stream()
                .map(stock -> mapToStockPrice(stock, priceMap.get(stock.getTicker())))
                .toList();
    }

    private List<StockPrice> processHistorical(List<Stock> stocks) {
        LocalDate startDate = targetDate.minusDays(14);
        return stocks.stream()
                .map(stock -> {
                    List<KisDailyPriceDetail> rows = kisDailyPricePort.fetchDailyPrices(stock, startDate, targetDate);
                    if (rows == null) {
                        throw new IllegalStateException("KIS 일봉 응답이 없습니다: " + stock.getTicker());
                    }

                    List<KisDailyPriceDetail> targetRows = rows.stream()
                            .filter(Objects::nonNull)
                            .filter(row -> targetDate.equals(row.baseDate()))
                            .toList();
                    if (targetRows.size() != 1) {
                        throw new IllegalStateException("KIS 일봉 응답에 기준일 데이터가 하나가 아닙니다: "
                                + stock.getTicker() + ", " + targetDate);
                    }

                    BigDecimal previousClose = rows.stream()
                            .filter(Objects::nonNull)
                            .filter(row -> row.baseDate() != null && row.baseDate().isBefore(targetDate))
                            .max(Comparator.comparing(KisDailyPriceDetail::baseDate))
                            .map(KisDailyPriceDetail::closePrice)
                            .orElse(null);
                    return mapToStockPrice(stock, targetRows.getFirst(), previousClose);
                })
                .toList();
    }

    private StockPrice mapToStockPrice(
            Stock stock,
            KisMultiStockPriceDetail dto
    ) {
        return StockPrice.of(
                stock,
                targetDate,
                dto.openPrice(),
                dto.highPrice(),
                dto.lowPrice(),
                dto.closePrice(),
                dto.closePrice(),
                dto.previousClosePrice(),
                dto.accumulatedVolume(),
                dto.accumulatedTradingValue(),
                TechnicalIndicators.empty()
        );
    }

    private StockPrice mapToStockPrice(Stock stock, KisDailyPriceDetail dto, BigDecimal previousClose) {
        return StockPrice.of(
                stock,
                targetDate,
                dto.openPrice(),
                dto.highPrice(),
                dto.lowPrice(),
                dto.closePrice(),
                dto.closePrice(),
                previousClose,
                dto.volume(),
                dto.transactionAmt(),
                TechnicalIndicators.empty()
        );
    }
}

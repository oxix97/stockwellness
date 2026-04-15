//package org.stockwellness.application.stockprice.service;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
//import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
//import org.stockwellness.application.port.out.stock.DailyStockPriceSnapshot;
//import org.stockwellness.application.port.out.stock.StockPricePort;
//import org.stockwellness.domain.stock.Stock;
//import org.stockwellness.domain.stock.price.StockPrice;
//import org.stockwellness.domain.stock.price.TechnicalIndicators;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//public class StockPriceFetchStepService extends AbstractStockPriceBatchStepService {
//
//    public StockPriceFetchStepService(StockPricePort stockPricePort) {
//        super(stockPricePort);
//    }
//
//    public List<StockPrice> fetchPrices(StockPriceSyncUseCase.StockPriceBatchCommand command) {
//        if (isEmptyStocks(command.stocks())) {
//            return List.of();
//        }
//
//        BatchDateContext dateContext = resolveBatchDateContext(command);
//        StockSyncPartition partition = partitionStocks(command.stocks(), dateContext);
//
//        List<StockPrice> resultEntities = new ArrayList<>();
//
//        if (!partition.todaySyncStocks().isEmpty()) {
//            resultEntities.addAll(fetchTodayPrices(
//                    partition.todaySyncStocks(),
//                    partition.latestDatesMap(),
//                    dateContext
//            ));
//        }
//
//        for (Stock stock : partition.gapSyncStocks()) {
//            resultEntities.addAll(fetchGapPrices(
//                    stock,
//                    partition.latestDatesMap().get(stock.getId()),
//                    dateContext
//            ));
//        }
//
//        return resultEntities;
//    }
//
//    private List<StockPrice> fetchTodayPrices(List<Stock> stocks, Map<Long, LocalDate> latestDatesMap, BatchDateContext dateContext) {
//        if (dateContext.effectiveBusinessDate().isBefore(EARLIEST_BASE_DATE)) {
//            return List.of();
//        }
//
//        List<String> requestedTickers = stocks.stream()
//                .map(Stock::getTicker)
//                .toList();
//
//        List<KisMultiStockPriceDetail> apiResults = executeKisCall(
//                new KisCallContext("multi-stock-prices", requestedTickers, null, dateContext.effectiveBusinessDate()),
//                () -> stockPricePort.fetchMultiStockPrices(requestedTickers)
//        );
//
//        Map<String, KisMultiStockPriceDetail> apiResultMap = (apiResults == null ? List.<KisMultiStockPriceDetail>of() : apiResults).stream()
//                .filter(detail -> detail.ticker() != null)
//                .collect(Collectors.toMap(
//                        KisMultiStockPriceDetail::ticker,
//                        detail -> detail,
//                        (left, right) -> left,
//                        LinkedHashMap::new
//                ));
//
//        List<StockPrice> entities = new ArrayList<>();
//        List<Stock> fallbackStocks = new ArrayList<>();
//        List<String> invalidPriceTickers = new ArrayList<>();
//
//        for (Stock stock : stocks) {
//            KisMultiStockPriceDetail todayPrice = apiResultMap.get(stock.getTicker());
//            if (todayPrice == null) {
//                fallbackStocks.add(stock);
//                continue;
//            }
//            if (todayPrice.closePrice() == null || todayPrice.closePrice().compareTo(BigDecimal.ZERO) <= 0) {
//                invalidPriceTickers.add(stock.getTicker());
//                fallbackStocks.add(stock);
//                continue;
//            }
//
//            entities.add(StockPrice.of(
//                    stock,
//                    dateContext.effectiveBusinessDate(),
//                    todayPrice.openPrice(),
//                    todayPrice.highPrice(),
//                    todayPrice.lowPrice(),
//                    todayPrice.closePrice(),
//                    todayPrice.closePrice(),
//                    BigDecimal.ZERO,
//                    todayPrice.accumulatedVolume(),
//                    todayPrice.accumulatedTradingValue(),
//                    TechnicalIndicators.empty()
//            ));
//        }
//
//        List<String> fallbackTickers = fallbackStocks.stream().map(Stock::getTicker).toList();
//        log.info("[KIS 배치] 멀티 시세 추적 requestedCount={}, respondedCount={}, fallbackCount={}, savedCount={}, effectiveBusinessDate={}, requestedTickers={}",
//                requestedTickers.size(),
//                apiResultMap.size(),
//                fallbackStocks.size(),
//                entities.size(),
//                dateContext.effectiveBusinessDate(),
//                requestedTickers);
//        if (!invalidPriceTickers.isEmpty()) {
//            log.warn("[KIS 배치] 멀티 시세 무효 가격 tickerCount={}, tickers={}", invalidPriceTickers.size(), invalidPriceTickers);
//        }
//        if (!fallbackTickers.isEmpty()) {
//            log.warn("[KIS 배치] 멀티 시세 fallback tickerCount={}, tickers={}", fallbackTickers.size(), fallbackTickers);
//        }
//
//        for (Stock fallbackStock : fallbackStocks) {
//            entities.addAll(fetchGapPrices(fallbackStock, latestDatesMap.get(fallbackStock.getId()), dateContext));
//        }
//
//        return entities;
//    }
//
//    private List<StockPrice> fetchGapPrices(Stock stock, LocalDate latestBaseDate, BatchDateContext dateContext) {
//        LocalDate storeStartDate = resolveStoreStartDate(
//                latestBaseDate,
//                dateContext.effectiveStartDate(),
//                dateContext.effectiveBusinessDate()
//        );
//        if (storeStartDate.isAfter(dateContext.effectiveBusinessDate())) {
//            return List.of();
//        }
//
//        List<DailyStockPriceSnapshot> priceSnapshots = fetchPricesFromPort(stock, storeStartDate, dateContext.effectiveBusinessDate());
//        if (priceSnapshots.isEmpty()) {
//            return List.of();
//        }
//
//        return priceSnapshots.stream()
//                .filter(snapshot -> snapshot.baseDate() != null)
//                .filter(snapshot -> !snapshot.baseDate().isBefore(EARLIEST_BASE_DATE))
//                .map(snapshot -> StockPrice.of(
//                        stock,
//                        snapshot.baseDate(),
//                        snapshot.openPrice(),
//                        snapshot.highPrice(),
//                        snapshot.lowPrice(),
//                        snapshot.closePrice(),
//                        snapshot.closePrice(),
//                        BigDecimal.ZERO,
//                        snapshot.volume(),
//                        snapshot.transactionAmt(),
//                        TechnicalIndicators.empty()
//                ))
//                .filter(Objects::nonNull)
//                .toList();
//    }
//}

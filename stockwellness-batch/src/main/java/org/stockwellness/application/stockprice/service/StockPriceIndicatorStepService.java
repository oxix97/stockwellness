//package org.stockwellness.application.stockprice.service;
//
//import org.springframework.stereotype.Service;
//import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
//import org.stockwellness.application.port.out.stock.StockPricePort;
//import org.stockwellness.domain.stock.Stock;
//import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
//import org.stockwellness.domain.stock.price.StockPrice;
//import org.stockwellness.domain.stock.price.TechnicalIndicators;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class StockPriceIndicatorStepService extends AbstractStockPriceBatchStepService {
//
//    public StockPriceIndicatorStepService(StockPricePort stockPricePort) {
//        super(stockPricePort);
//    }
//
//    public List<StockPrice> calculateIndicators(StockPriceSyncUseCase.StockPriceBatchCommand command) {
//        if (isEmptyStocks(command.stocks())) {
//            return List.of();
//        }
//
//        BatchDateContext dateContext = resolveBatchDateContext(command);
//        LocalDate lookbackBaseDate = dateContext.effectiveStartDate() != null
//                ? dateContext.effectiveStartDate()
//                : dateContext.effectiveBusinessDate();
//        Map<Long, List<StockPrice>> historicalEntitiesMap = stockPricePort.findRecentPricesWithDateByStocks(
//                command.stocks(),
//                lookbackBaseDate,
//                INDICATOR_BUFFER_DAYS + 10
//        );
//
//        List<StockPrice> updatedEntities = new ArrayList<>();
//        for (Stock stock : command.stocks()) {
//            List<StockPrice> allPrices = new ArrayList<>(historicalEntitiesMap.getOrDefault(stock.getId(), List.of()));
//            if (allPrices.isEmpty()) {
//                continue;
//            }
//
//            allPrices.sort(Comparator.comparing(price -> price.getId().getBaseDate()));
//
//            List<LocalDate> dates = allPrices.stream().map(price -> price.getId().getBaseDate()).toList();
//            List<BigDecimal> highPrices = allPrices.stream()
//                    .map(price -> price.getHighPrice() != null ? price.getHighPrice() : price.getClosePrice())
//                    .toList();
//            List<BigDecimal> lowPrices = allPrices.stream()
//                    .map(price -> price.getLowPrice() != null ? price.getLowPrice() : price.getClosePrice())
//                    .toList();
//            List<BigDecimal> closePrices = allPrices.stream()
//                    .map(price -> price.getClosePrice() != null ? price.getClosePrice() : BigDecimal.ZERO)
//                    .toList();
//
//            List<TechnicalIndicators> indicatorsList = TechnicalIndicatorCalculator.calculateSeries(
//                    highPrices,
//                    lowPrices,
//                    closePrices,
//                    dates
//            );
//
//            for (int i = 0; i < allPrices.size(); i++) {
//                StockPrice price = allPrices.get(i);
//                if (shouldUpdateIndicators(price, dateContext)) {
//                    BigDecimal prevClose = i > 0 ? allPrices.get(i - 1).getClosePrice() : BigDecimal.ZERO;
//                    price.updateIndicators(indicatorsList.get(i), prevClose);
//                    updatedEntities.add(price);
//                }
//            }
//        }
//
//        return updatedEntities;
//    }
//}

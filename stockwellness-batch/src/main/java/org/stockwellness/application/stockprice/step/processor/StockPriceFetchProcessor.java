//package org.stockwellness.application.stockprice.step.processor;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.beans.factory.annotation.Value;
//import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
//import org.stockwellness.application.stockprice.service.StockPriceFetchStepService;
//import org.stockwellness.domain.stock.Stock;
//import org.stockwellness.domain.stock.price.StockPrice;
//
//import java.util.List;
//
//@RequiredArgsConstructor
//public class StockPriceFetchProcessor implements ItemProcessor<List<Stock>, List<StockPrice>> {
//
//    private final StockPriceFetchStepService stockPriceFetchStepService;
//
//    @Value("#{jobParameters['startDate']}")
//    private String startDateStr;
//
//    @Value("#{jobParameters['endDate']}")
//    private String endDateStr;
//
//    @Override
//    public List<StockPrice> process(List<Stock> stocks) {
//        return stockPriceFetchStepService.fetchPrices(
//                new StockPriceSyncUseCase.StockPriceBatchCommand(stocks, startDateStr, endDateStr)
//        );
//    }
//}

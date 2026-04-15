//package org.stockwellness.application.stockprice.service;
//
//import org.springframework.stereotype.Service;
//import org.stockwellness.application.port.in.batch.StockPriceRepairUseCase;
//import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
//import org.stockwellness.application.port.out.stock.StockPricePort;
//import org.stockwellness.domain.stock.price.StockPrice;
//import org.stockwellness.global.util.DateUtil;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class StockPriceBatchService implements StockPriceSyncUseCase, StockPriceRepairUseCase {
//
//    private final StockPricePort stockPricePort;
//    private final StockInvestorTradeStepService stockInvestorTradeStepService;
//    private final StockPriceFetchStepService stockPriceFetchStepService;
//    private final StockPriceIndicatorStepService stockPriceIndicatorStepService;
//
//    public StockPriceBatchService(
//            StockPricePort stockPricePort,
//            StockInvestorTradeStepService stockInvestorTradeStepService,
//            StockPriceFetchStepService stockPriceFetchStepService,
//            StockPriceIndicatorStepService stockPriceIndicatorStepService
//    ) {
//        this.stockPricePort = stockPricePort;
//        this.stockInvestorTradeStepService = stockInvestorTradeStepService;
//        this.stockPriceFetchStepService = stockPriceFetchStepService;
//        this.stockPriceIndicatorStepService = stockPriceIndicatorStepService;
//    }
//
//    @Override
//    public StockPriceSyncResult sync(StockPriceBatchCommand command) {
//        fetchInvestorTrades(command);
//        fetch(command);
//        return calculateIndicators(new StockPriceBatchCommand(command.stocks(), command.startDate(), command.endDate()));
//    }
//
//    /**
//     * [Step 2-1] 수급 수집: 국내기관_외국인 매매종목가집계를 통해 투자주체 수급 데이터만 수집하여 반환
//     */
//    @Override
//    public StockInvestorTradeSyncResult fetchInvestorTrades(StockPriceBatchCommand command) {
//        return new StockInvestorTradeSyncResult(stockInvestorTradeStepService.fetchInvestorTrades(command));
//    }
//
//    /**
//     * [Step 2-2] 시세 수집: 기준일이 오늘일 때만 멀티 시세를 사용하고, 그 외에는 일봉 데이터만 수집한다.
//     */
//    @Override
//    public StockPriceSyncResult fetch(StockPriceBatchCommand command) {
//        return new StockPriceSyncResult(stockPriceFetchStepService.fetchPrices(command));
//    }
//
//    /**
//     * [Step 2-3] 지표 계산: 같은 기준일 정책으로 수집된 시세를 대상으로 지표를 계산한다.
//     */
//    @Override
//    public StockPriceSyncResult calculateIndicators(StockPriceBatchCommand command) {
//        return new StockPriceSyncResult(stockPriceIndicatorStepService.calculateIndicators(command));
//    }
//
//    @Override
//    public StockPriceRepairResult repair(StockPriceRepairCommand command) {
//        List<StockPrice> allPrices = stockPricePort.findRecentPricesWithDateByStocks(
//                List.of(command.stock()),
//                LocalDate.now(),
//                Integer.MAX_VALUE
//        ).getOrDefault(command.stock().getId(), List.of());
//        if (allPrices.isEmpty()) return new StockPriceRepairResult(List.of());
//
//        LocalDate reqStart = DateUtil.parse(command.startDate());
//        LocalDate reqEnd = DateUtil.parse(command.endDate());
//        List<StockPriceRepairRow> toUpdate = new ArrayList<>();
//        BigDecimal previousClose = null;
//
//        for (StockPrice current : allPrices) {
//            LocalDate currentBaseDate = current.getId().getBaseDate();
//            if (previousClose != null) {
//                boolean inRange = DateUtil.isBetween(currentBaseDate, reqStart, reqEnd);
//                boolean needsRepair = current.getPreviousClosePrice() == null || current.getPreviousClosePrice().compareTo(BigDecimal.ZERO) == 0;
//                if (inRange && needsRepair) {
//                    toUpdate.add(new StockPriceRepairRow(command.stock().getId(), currentBaseDate, previousClose));
//                }
//            }
//            previousClose = current.getClosePrice();
//        }
//        return new StockPriceRepairResult(toUpdate);
//    }
//
//}

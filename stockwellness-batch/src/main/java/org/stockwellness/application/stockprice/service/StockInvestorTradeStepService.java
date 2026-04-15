//package org.stockwellness.application.stockprice.service;
//
//import org.springframework.stereotype.Service;
//import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
//import org.stockwellness.application.port.out.stock.InvestorTradingSnapshot;
//import org.stockwellness.application.port.out.stock.StockPricePort;
//import org.stockwellness.domain.stock.Stock;
//import org.stockwellness.domain.stock.price.StockInvestorTrade;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class StockInvestorTradeStepService extends AbstractStockPriceBatchStepService {
//
//    public StockInvestorTradeStepService(StockPricePort stockPricePort) {
//        super(stockPricePort);
//    }
//
//    public List<StockInvestorTrade> fetchInvestorTrades(StockPriceSyncUseCase.StockPriceBatchCommand command) {
//        if (isEmptyStocks(command.stocks())) {
//            return List.of();
//        }
//
//        BatchDateContext dateContext = resolveBatchDateContext(command);
//        StockSyncPartition partition = partitionStocks(command.stocks(), dateContext);
//
//        List<StockInvestorTrade> resultInvestorTrades = new ArrayList<>();
//
//        for (Stock stock : partition.todaySyncStocks()) {
//            resultInvestorTrades.addAll(fetchInvestorTradesForRange(
//                    stock,
//                    dateContext.effectiveBusinessDate(),
//                    dateContext.effectiveBusinessDate()
//            ));
//        }
//
//        for (Stock stock : partition.gapSyncStocks()) {
//            resultInvestorTrades.addAll(fetchInvestorTradesForRange(
//                    stock,
//                    resolveStoreStartDate(
//                            partition.latestDatesMap().get(stock.getId()),
//                            dateContext.effectiveStartDate(),
//                            dateContext.effectiveBusinessDate()
//                    ),
//                    dateContext.effectiveBusinessDate()
//            ));
//        }
//
//        return resultInvestorTrades;
//    }
//
//    private List<StockInvestorTrade> fetchInvestorTradesForRange(Stock stock, LocalDate startDate, LocalDate endDate) {
//        if (startDate.isAfter(endDate)) {
//            return List.of();
//        }
//
//        List<InvestorTradingSnapshot> investorSnapshots = executeKisCall(
//                KisCallContext.of("investor-prices", stock.getTicker(), startDate, endDate),
//                () -> stockPricePort.fetchInvestorTradingSnapshots(stock, startDate, endDate)
//        );
//        if (investorSnapshots == null || investorSnapshots.isEmpty()) {
//            return List.of();
//        }
//
//        return investorSnapshots.stream()
//                .map(snapshot -> StockInvestorTrade.of(
//                        stock, snapshot.baseDate(), stock.getName(), stock.getTicker(),
//                        snapshot.netForeignBuyingQty(), snapshot.netInstitutionalBuyingQty(), snapshot.netPersonBuyingQty(),
//                        null, null, null, null, null, null,
//                        snapshot.netForeignBuyingAmt(), snapshot.netInstitutionalBuyingAmt(), snapshot.netPersonBuyingAmt(),
//                        null, null, null, null, null, null
//                ))
//                .toList();
//    }
//}

package org.stockwellness.application.stockprice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class StockPriceSyncService {

    private final StockPort stockPort;
    private final StockPricePort stockPricePort;
    private final KisDailyPriceAdapter kisPriceAdapter;

    /**
     * 당일 종목 가격 반영<br>
     * 1. ACTIVE 상태의 종목을 전부 가져온다. <br>
     * 2. 최대 30개씩 끊어서 fetchMultiStockPrices 에 List<String> tickers 로 전달 <br>
     * 3. KisMultiStockPriceDetail -> StockPrice로 변환 및 저장
     */
    @Transactional
    public void saveDailyStockPrices() {
        List<Stock> stocks = stockPort.findAllByActiveStocks();
        log.info("[KIS 시세 저장] 시작 - 대상 종목 수: {}", stocks.size());

        int pageSize = 30;
        int totalStocks = stocks.size();
        LocalDate baseDate = LocalDate.now();

        for (int i = 0; i < totalStocks; i += pageSize) {
            int end = Math.min(i + pageSize, totalStocks);
            List<Stock> subList = stocks.subList(i, end);
            List<String> tickers = subList.stream().map(Stock::getTicker).toList();

            List<KisMultiStockPriceDetail> dtos = kisPriceAdapter.fetchMultiStockPrices(tickers);

            Map<String, Stock> stockMap = subList.stream()
                    .collect(Collectors.toMap(Stock::getTicker, Function.identity()));

            List<StockPrice> stockPrices = toStockPrices(dtos, stockMap, baseDate);
            stockPricePort.saveAll(stockPrices);
        }
        log.info("[KIS 시세 저장] 완료");
    }

    private List<StockPrice> toStockPrices(List<KisMultiStockPriceDetail> dtos, Map<String, Stock> stockMap, LocalDate baseDate) {
        return dtos.stream()
                .filter(dto -> stockMap.containsKey(dto.ticker()))
                .map(dto -> {
                    Stock stock = stockMap.get(dto.ticker());
                    return StockPrice.of(
                            stock,
                            baseDate,
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
                })
                .toList();
    }
}

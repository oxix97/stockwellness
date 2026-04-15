package org.stockwellness.application.stockprice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class StockPriceFacade {

    private final StockPriceSyncService stockPriceSyncService;
    private final StockPriceCalculateService stockPriceCalculateService;

    public void syncStockPrice() {
        stockPriceSyncService.saveDailyStockPrices();

        stockPriceCalculateService.calculateStockPrice();
    }
}

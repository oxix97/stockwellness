package org.stockwellness.adapter.in.web.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.application.stockprice.service.StockPriceFacade;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stock")
public class StockSyncController {
    private final StockPriceFacade stockPriceFacade;

    @PostMapping("/price-fetch")
    public void fetchPrice() {
        stockPriceFacade.syncStockPrice();
    }

}

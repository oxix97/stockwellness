package org.stockwellness.adapter.in.web.stock;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.stock.dto.StockDetailResponse;
import org.stockwellness.adapter.in.web.stock.dto.StockResponse;
import org.stockwellness.adapter.in.web.stock.dto.StockSearchRequest;
import org.stockwellness.application.service.StockReadService;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/stocks")
@RestController
public class StockController {

    private final StockReadService stockReadService;

    @GetMapping
    public ResponseEntity<Slice<StockResponse>> searchStocks(
            @Valid @ModelAttribute StockSearchRequest request
    ) {
        var response = stockReadService.searchStocks(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<StockDetailResponse> getStockDetail(
            @PathVariable("ticker") String ticker
    ) {
        var response = stockReadService.getStockDetail(ticker);
        return ResponseEntity.ok(response);
    }


}

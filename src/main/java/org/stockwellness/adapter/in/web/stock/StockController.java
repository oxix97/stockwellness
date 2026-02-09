package org.stockwellness.adapter.in.web.stock;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.stock.dto.StockAnalysisResponse;
import org.stockwellness.adapter.in.web.stock.dto.StockDetailResponse;
import org.stockwellness.adapter.in.web.stock.dto.StockResponse;
import org.stockwellness.adapter.in.web.stock.dto.StockSearchRequest;
import org.stockwellness.application.port.in.stock.command.StockAnalysisCommand;
import org.stockwellness.application.port.in.stock.StockAnalysisUseCase;
import org.stockwellness.application.port.in.stock.StockReadUseCase;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;
import org.stockwellness.application.port.in.stock.result.StockAnalysisResult;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/stocks")
@RestController
public class StockController {
    private final StockReadUseCase stockReadUseCase;
    private final StockAnalysisUseCase stockAnalysisUseCase;

    @GetMapping
    public ResponseEntity<Slice<StockResponse>> searchStocks(
            @Valid @ModelAttribute StockSearchRequest request
    ) {
        Slice<StockSearchResult> results = stockReadUseCase.searchStocks(request.toQuery());

        var response = results.map(StockResponse::from);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<StockDetailResponse> getStockDetail(
            @PathVariable("ticker") String ticker
    ) {
        StockDetailResult result = stockReadUseCase.getStockDetail(ticker);

        var response = StockDetailResponse.from(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/analysis")
    public ResponseEntity<StockAnalysisResponse> getAiAnalysis(
            @RequestParam(name = "isin_code") String isinCode
    ) {
        StockAnalysisCommand command = new StockAnalysisCommand(isinCode);

        StockAnalysisResult result = stockAnalysisUseCase.analyze(command);

        var response = StockAnalysisResponse.from(result);
        return ResponseEntity.ok(response);
    }
}

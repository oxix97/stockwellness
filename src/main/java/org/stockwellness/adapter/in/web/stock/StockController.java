package org.stockwellness.adapter.in.web.stock;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.stock.dto.StockAnalysisResponse;
import org.stockwellness.adapter.in.web.stock.dto.StockDetailResponse;
import org.stockwellness.adapter.in.web.stock.dto.StockResponse;
import org.stockwellness.adapter.in.web.stock.dto.StockSearchRequest;
import org.stockwellness.application.port.in.stock.StockAnalysisCommand;
import org.stockwellness.application.port.out.stock.StockAnalysisResult;
import org.stockwellness.application.port.in.stock.StockAnalysisUseCase;
import org.stockwellness.application.service.StockReadService;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/stocks")
@RestController
public class StockController {
    private final StockReadService stockReadService;
    private final StockAnalysisUseCase stockAnalysisUseCase;

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

    @GetMapping("/analysis")
    public ResponseEntity<StockAnalysisResponse> getAiAnalysis(
            @Pattern(regexp = "^[0-9A-Za-z]{4,12}$", message = "올바른 종목 코드가 아닙니다.")
            @RequestParam(name = "isin_code") String isinCode
    ) {
        log.info("📥 Web Request: Analyze Stock [{}]", isinCode);

        // 1. Command 객체 생성 (Input Mapping)
        StockAnalysisCommand command = new StockAnalysisCommand(isinCode);

        // 2. UseCase 실행 (Business Logic)
        StockAnalysisResult result = stockAnalysisUseCase.analyze(command);

        // 3. Response DTO 변환 (Output Mapping)
        return ResponseEntity.ok(StockAnalysisResponse.from(result));
    }
}

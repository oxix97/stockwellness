package org.stockwellness.adapter.in.web.market;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.application.port.in.stock.MarketIndexUseCase;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult;
import org.stockwellness.global.common.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketIndexUseCase marketIndexUseCase;

    /**
     * 시장 지수 목록 조회 (KOSPI · KOSDAQ · S&P500)
     */
    @GetMapping("/indexes")
    public ApiResponse<List<MarketIndexResult>> getMarketIndexes() {
        return ApiResponse.success(marketIndexUseCase.getMarketIndexes());
    }
}

package org.stockwellness.adapter.out.persistence.krx;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.external.krx.KrxClient;
import org.stockwellness.adapter.out.external.krx.dto.KrxCommonResponse;
import org.stockwellness.adapter.out.external.krx.dto.KrxListedInfoResponse;
import org.stockwellness.adapter.out.external.krx.dto.StockPriceDto;
import org.stockwellness.application.port.out.FetchStockPricePort;
import org.stockwellness.application.service.mapper.StockMapper;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KrxStockAdapter implements FetchStockPricePort {
    private final KrxClient krxClient;
    private final StockMapper stockMapper;

    @Override
    public List<Stock> fetchDaily(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        KrxListedInfoResponse response = krxClient.stockInfos(dateStr);

        return List.of();
    }

    @Override
    public List<StockHistory> fetchDailyPrice(LocalDate date) {
        // 1. 날짜 포맷팅 (LocalDate -> YYYYMMDD String)
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 2. API 호출
        KrxCommonResponse<StockPriceDto> response = krxClient.stockPriceResponse(dateStr);
        // 4. DTO -> Domain Entity 변환
        return response.getItems().stream()
                .map(stockMapper::toHistoryEntity)
                .toList();
    }
}

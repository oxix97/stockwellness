package org.stockwellness.batch.job.stockprice.sync.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.stockwellness.batch.job.stockprice.sync.support.StockPriceTestFixture.createSamsungStock;

class StockPriceBatchServiceTest {

    private StockPricePort stockPricePort;
    private StockPriceBatchService stockPriceBatchService;
    private Stock samsung;

    @BeforeEach
    void setUp() {
        stockPricePort = Mockito.mock(StockPricePort.class);
        stockPriceBatchService = new StockPriceBatchService(stockPricePort);
        samsung = createSamsungStock();
    }

    @Test
    @DisplayName("멀티 종목 시세 조회 중 재시도 대상 업무 오류는 그대로 전파된다")
    void syncPropagatesRetryableBusinessExceptionFromMultiPriceCall() {
        LocalDate endDate = LocalDate.of(2026, 4, 10);

        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Map.of(samsung.getId(), endDate));
        when(stockPricePort.findRecentPricesWithDateByStocks(any(), eq(endDate), eq(120))).thenReturn(Collections.emptyMap());
        when(stockPricePort.fetchMultiStockPrices(List.of(samsung.getTicker())))
                .thenThrow(new KisApiException("1", "EGW00316", "조회 처리 중 오류 발생하였습니다. 재 조회 수행 부탁드립니다."));

        assertThatThrownBy(() -> stockPriceBatchService.sync(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), null, "20260410")
        )).isInstanceOf(KisApiException.class)
                .hasMessageContaining("EGW00316");
    }


}

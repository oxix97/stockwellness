package org.stockwellness.batch.job.stockprice.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.DailyStockPriceSnapshot;
import org.stockwellness.application.port.out.stock.InvestorTradingSnapshot;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.stockprice.service.StockInvestorTradeStepService;
import org.stockwellness.application.stockprice.service.StockPriceBatchService;
import org.stockwellness.application.stockprice.service.StockPriceFetchStepService;
import org.stockwellness.application.stockprice.service.StockPriceIndicatorStepService;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.stockwellness.batch.job.stockprice.support.StockPriceTestFixture.createSamsungStock;

class StockPriceBatchServiceTest {

    private StockPricePort stockPricePort;
    private StockPriceBatchService stockPriceBatchService;
    private StockInvestorTradeStepService stockInvestorTradeStepService;
    private StockPriceFetchStepService stockPriceFetchStepService;
    private StockPriceIndicatorStepService stockPriceIndicatorStepService;
    private Stock samsung;

    @BeforeEach
    void setUp() {
        stockPricePort = Mockito.mock(StockPricePort.class);
        stockInvestorTradeStepService = Mockito.spy(new StockInvestorTradeStepService(stockPricePort));
        stockPriceFetchStepService = Mockito.spy(new StockPriceFetchStepService(stockPricePort));
        stockPriceIndicatorStepService = Mockito.spy(new StockPriceIndicatorStepService(stockPricePort));
        stockPriceBatchService = new StockPriceBatchService(
                stockPricePort,
                stockInvestorTradeStepService,
                stockPriceFetchStepService,
                stockPriceIndicatorStepService
        );
        samsung = createSamsungStock();
    }

    @Test
    @DisplayName("멀티 종목 시세 조회 중 재시도 대상 업무 오류는 그대로 전파된다")
    void syncPropagatesRetryableBusinessExceptionFromMultiPriceCall() {
        LocalDate today = LocalDate.of(2026, 4, 10);
        Mockito.doReturn(today).when(stockInvestorTradeStepService).currentDate();
        Mockito.doReturn(today).when(stockPriceFetchStepService).currentDate();
        Mockito.doReturn(today).when(stockPriceIndicatorStepService).currentDate();

        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Map.of(samsung.getId(), today.minusDays(1)));
        when(stockPricePort.fetchInvestorTradingSnapshots(eq(samsung), any(), any())).thenReturn(List.of());
        when(stockPricePort.fetchMultiStockPrices(List.of(samsung.getTicker())))
                .thenThrow(new KisApiException("1", "EGW00316", "조회 처리 중 오류 발생하였습니다. 재 조회 수행 부탁드립니다."));

        assertThatThrownBy(() -> stockPriceBatchService.sync(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), null, "20260410")
        )).isInstanceOf(KisApiException.class)
                .hasMessageContaining("EGW00316");
    }

    @Test
    @DisplayName("수급 수집 시 투자자별 수급 데이터가 함께 수집되어야 한다")
    void fetchInvestorTradesCollectsInvestorData() {
        LocalDate today = LocalDate.of(2026, 4, 14);
        Mockito.doReturn(today).when(stockInvestorTradeStepService).currentDate();
        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Map.of(samsung.getId(), today.minusDays(1)));

        InvestorTradingSnapshot investor = new InvestorTradingSnapshot(
                today,
                BigDecimal.valueOf(70500), BigDecimal.valueOf(0.5), 1000000L,
                10000L, 20000L, -30000L,
                BigDecimal.valueOf(705000000L), BigDecimal.valueOf(1410000000L), BigDecimal.valueOf(-2115000000L)
        );
        when(stockPricePort.fetchInvestorTradingSnapshots(eq(samsung), any(), any())).thenReturn(List.of(investor));

        StockPriceSyncUseCase.StockInvestorTradeSyncResult result = stockPriceBatchService.fetchInvestorTrades(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), "20260414", "20260414")
        );

        assertThat(result.investorTrades()).hasSize(1);
        assertThat(result.investorTrades().getFirst().getFrgnNtbyTrPbmn()).isEqualByComparingTo(investor.netForeignBuyingAmt());
    }

    @Test
    @DisplayName("endDate가 오늘이면 멀티 시세 결과를 기준일로 저장한다")
    void fetchUsesMultiPriceApiWhenEndDateIsToday() {
        LocalDate today = LocalDate.of(2026, 4, 14);
        Mockito.doReturn(today).when(stockPriceFetchStepService).currentDate();
        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Map.of(samsung.getId(), today.minusDays(1)));
        when(stockPricePort.fetchMultiStockPrices(List.of(samsung.getTicker()))).thenReturn(List.of(
                new KisMultiStockPriceDetail(
                        samsung.getTicker(),
                        samsung.getName(),
                        BigDecimal.valueOf(70500),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(70000),
                        BigDecimal.valueOf(71000),
                        BigDecimal.valueOf(69000),
                        1000000L,
                        BigDecimal.valueOf(70500000000L),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )
        ));

        StockPriceSyncUseCase.StockPriceSyncResult result = stockPriceBatchService.fetch(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), null, "20260414")
        );

        assertThat(result.stockPrices()).hasSize(1);
        assertThat(result.stockPrices().getFirst().getId().getBaseDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("오늘 멀티 시세 응답에서 빠진 종목은 일봉 조회로 즉시 보완한다")
    void fetchFallsBackToDailyPricesWhenMultiResponseMissesTicker() {
        LocalDate today = LocalDate.of(2026, 4, 14);
        Mockito.doReturn(today).when(stockPriceFetchStepService).currentDate();
        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Map.of(samsung.getId(), today.minusDays(1)));
        when(stockPricePort.fetchMultiStockPrices(List.of(samsung.getTicker()))).thenReturn(List.of());
        when(stockPricePort.fetchDailyPrices(eq(samsung), eq(today), eq(today))).thenReturn(List.of(
                new DailyStockPriceSnapshot(
                        today,
                        BigDecimal.valueOf(70000),
                        BigDecimal.valueOf(71000),
                        BigDecimal.valueOf(69000),
                        BigDecimal.valueOf(70500),
                        1000000L,
                        BigDecimal.valueOf(70500000000L)
                )
        ));

        StockPriceSyncUseCase.StockPriceSyncResult result = stockPriceBatchService.fetch(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), null, "20260414")
        );

        assertThat(result.stockPrices()).hasSize(1);
        assertThat(result.stockPrices().getFirst().getId().getBaseDate()).isEqualTo(today);
        Mockito.verify(stockPricePort).fetchDailyPrices(eq(samsung), eq(today), eq(today));
    }

    @Test
    @DisplayName("endDate가 오늘이 아니면 멀티 시세를 사용하지 않고 일봉 조회만 사용한다")
    void fetchSkipsMultiPriceApiWhenEndDateIsNotToday() {
        LocalDate today = LocalDate.of(2026, 4, 15);
        LocalDate endDate = LocalDate.of(2026, 4, 14);
        Mockito.doReturn(today).when(stockPriceFetchStepService).currentDate();
        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Map.of(samsung.getId(), endDate.minusDays(1)));
        when(stockPricePort.fetchDailyPrices(eq(samsung), eq(endDate), eq(endDate))).thenReturn(List.of(
                new DailyStockPriceSnapshot(
                        endDate,
                        BigDecimal.valueOf(70000),
                        BigDecimal.valueOf(71000),
                        BigDecimal.valueOf(69000),
                        BigDecimal.valueOf(70500),
                        1000000L,
                        BigDecimal.valueOf(70500000000L)
                )
        ));

        StockPriceSyncUseCase.StockPriceSyncResult result = stockPriceBatchService.fetch(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), null, "20260414")
        );

        assertThat(result.stockPrices()).hasSize(1);
        assertThat(result.stockPrices().getFirst().getId().getBaseDate()).isEqualTo(endDate);
        Mockito.verify(stockPricePort, Mockito.never()).fetchMultiStockPrices(any());
    }

    @Test
    @DisplayName("endDate가 오늘이 아니면 수급 수집도 기준일 범위만 사용한다")
    void fetchInvestorTradesUsesHistoricalRangeWhenEndDateIsNotToday() {
        LocalDate today = LocalDate.of(2026, 4, 15);
        LocalDate endDate = LocalDate.of(2026, 4, 14);
        Mockito.doReturn(today).when(stockInvestorTradeStepService).currentDate();
        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Map.of(samsung.getId(), endDate.minusDays(1)));

        InvestorTradingSnapshot investor = new InvestorTradingSnapshot(
                endDate,
                BigDecimal.valueOf(70500), BigDecimal.valueOf(0.5), 1000000L,
                10000L, 20000L, -30000L,
                BigDecimal.valueOf(705000000L), BigDecimal.valueOf(1410000000L), BigDecimal.valueOf(-2115000000L)
        );
        when(stockPricePort.fetchInvestorTradingSnapshots(eq(samsung), eq(endDate), eq(endDate))).thenReturn(List.of(investor));

        StockPriceSyncUseCase.StockInvestorTradeSyncResult result = stockPriceBatchService.fetchInvestorTrades(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), null, "20260414")
        );

        assertThat(result.investorTrades()).hasSize(1);
        assertThat(result.investorTrades().getFirst().getId().getBaseDate()).isEqualTo(endDate);
    }

    @Test
    @DisplayName("endDate가 오늘이면 지표 계산은 오늘 row를 대상으로 갱신한다")
    void calculateIndicatorsUsesTodayAsEffectiveBusinessDate() {
        LocalDate today = LocalDate.of(2026, 4, 14);
        Mockito.doReturn(today).when(stockPriceIndicatorStepService).currentDate();

        List<StockPrice> prices = List.of(
                StockPrice.of(
                        samsung, today.minusDays(1),
                        BigDecimal.valueOf(70000), BigDecimal.valueOf(71000), BigDecimal.valueOf(69000), BigDecimal.valueOf(70500),
                        BigDecimal.valueOf(70500), BigDecimal.ZERO,
                        1000000L, BigDecimal.valueOf(70500000000L),
                        TechnicalIndicators.empty()
                ),
                StockPrice.of(
                        samsung, today,
                        BigDecimal.valueOf(70500), BigDecimal.valueOf(71500), BigDecimal.valueOf(69500), BigDecimal.valueOf(71000),
                        BigDecimal.valueOf(71000), BigDecimal.ZERO,
                        1100000L, BigDecimal.valueOf(71000000000L),
                        TechnicalIndicators.empty()
                )
        );
        when(stockPricePort.findRecentPricesWithDateByStocks(any(), eq(today), anyInt())).thenReturn(Map.of(samsung.getId(), prices));

        StockPriceSyncUseCase.StockPriceSyncResult result = stockPriceBatchService.calculateIndicators(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), null, "20260414")
        );

        assertThat(result.stockPrices()).extracting(price -> price.getId().getBaseDate()).contains(today);
    }
}

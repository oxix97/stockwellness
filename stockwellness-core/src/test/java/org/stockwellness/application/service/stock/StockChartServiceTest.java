package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.stock.StockPriceUseCase.ChartQuery;
import org.stockwellness.application.port.in.stock.result.ChartDataResponse;
import org.stockwellness.application.port.in.stock.result.ReturnRateResponse;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.price.ChartFrequency;
import org.stockwellness.domain.stock.price.ChartPeriod;
import org.stockwellness.domain.stock.exception.StockPriceException;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StockChartServiceTest {

    @Mock
    private StockPricePort stockPricePort;
    @Mock
    private LoadBenchmarkPort loadBenchmarkPort;
    @Mock
    private StockPort stockPort;

    @InjectMocks
    private StockChartService stockChartService;

    private final String ticker = "AAPL";

    @Nested
    @DisplayName("차트 데이터 조회")
    class LoadChartData {

        @Test
        @DisplayName("성공: 유효한 티커와 기간으로 차트 데이터를 조회한다")
        void success() {
            // given
            ChartQuery query = new ChartQuery(ticker, ChartPeriod.ONE_YEAR, ChartFrequency.DAILY, true);
            given(stockPort.existsByTicker(ticker)).willReturn(true);
            
            List<StockPriceResult> mockPrices = List.of(
                    createPrice("2024-01-01", 100),
                    createPrice("2024-01-02", 110)
            );
            given(stockPricePort.loadPricesByTicker(eq(ticker), any(), any())).willReturn(mockPrices);
            
            List<StockPriceResult> mockBenchmarks = List.of(
                    createPrice("2024-01-01", 2000),
                    createPrice("2024-01-02", 2100)
            );
            given(loadBenchmarkPort.loadBenchmarkPrices(eq("^KS11"), any(), any())).willReturn(mockBenchmarks);

            // when
            ChartDataResponse response = stockChartService.loadChartData(query);

            // then
            assertThat(response.ticker()).isEqualTo(ticker);
            assertThat(response.prices()).hasSize(2);
            assertThat(response.benchmarks()).hasSize(2);
            assertThat(response.benchmarks().get(1).returnRate()).isEqualByComparingTo("5.0"); // (2100-2000)/2000 * 100
        }

        @Test
        @DisplayName("실패: 존재하지 않는 종목인 경우 예외가 발생한다")
        void failStockNotFound() {
            // given
            ChartQuery query = new ChartQuery("UNKNOWN", ChartPeriod.ONE_YEAR, ChartFrequency.DAILY, false);
            given(stockPort.existsByTicker("UNKNOWN")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> stockChartService.loadChartData(query))
                    .isInstanceOf(StockPriceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STOCK_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 해당 기간의 시세 데이터가 없는 경우 예외가 발생한다")
        void failNoPriceData() {
            // given
            ChartQuery query = new ChartQuery(ticker, ChartPeriod.ONE_YEAR, ChartFrequency.DAILY, false);
            given(stockPort.existsByTicker(ticker)).willReturn(true);
            given(stockPricePort.loadPricesByTicker(eq(ticker), any(), any())).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> stockChartService.loadChartData(query))
                    .isInstanceOf(StockPriceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRICE_DATA_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("수익률 계산")
    class CalculateReturn {

        @Test
        @DisplayName("성공: 종목 및 벤치마크 수익률을 계산한다")
        void success() {
            // given
            given(stockPort.existsByTicker(ticker)).willReturn(true);
            
            List<StockPriceResult> mockPrices = List.of(
                    createPrice("2024-01-01", 100),
                    createPrice("2024-01-02", 150)
            );
            given(stockPricePort.loadPricesByTicker(eq(ticker), any(), any())).willReturn(mockPrices);

            List<StockPriceResult> mockBenchmarks = List.of(
                    createPrice("2024-01-01", 2000),
                    createPrice("2024-01-02", 2200)
            );
            given(loadBenchmarkPort.loadBenchmarkPrices(eq("^KS11"), any(), any())).willReturn(mockBenchmarks);

            // when
            ReturnRateResponse response = stockChartService.calculateReturn(ticker, ChartPeriod.ONE_YEAR);

            // then
            assertThat(response.stockReturnRate()).isEqualByComparingTo("50.0");
            assertThat(response.benchmarkReturnRate()).isEqualByComparingTo("10.0");
        }
    }

    private StockPriceResult createPrice(String date, double adjClose) {
        return new StockPriceResult(
                LocalDate.parse(date),
                BigDecimal.valueOf(adjClose),
                BigDecimal.valueOf(adjClose),
                BigDecimal.valueOf(adjClose),
                BigDecimal.valueOf(adjClose),
                BigDecimal.valueOf(adjClose),
                100L
        );
    }
}

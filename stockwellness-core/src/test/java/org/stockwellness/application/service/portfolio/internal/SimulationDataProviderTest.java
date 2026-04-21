package org.stockwellness.application.service.portfolio.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.StockPricePort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimulationDataProvider 단위 테스트")
class SimulationDataProviderTest {

    @InjectMocks
    private SimulationDataProvider simulationDataProvider;

    @Mock
    private StockPricePort stockPricePort;

    @Mock
    private LoadBenchmarkPort loadBenchmarkPort;

    @Test
    @DisplayName("종목 리스트와 벤치마크 지수의 최근 2년 시세 데이터를 벌크 로딩한다")
    void load_simulation_data() {
        // given
        List<String> symbols = List.of("AAPL", "005930");
        List<String> benchmarkTickers = List.of("KOSPI");
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);

        StockPriceResult aaplPrice = new StockPriceResult(start, BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102), BigDecimal.valueOf(102), 1000L, null, null, null, null, null);
        StockPriceResult benchmarkPrice = new StockPriceResult(start, BigDecimal.valueOf(2500), BigDecimal.valueOf(2550), BigDecimal.valueOf(2450), BigDecimal.valueOf(2520), BigDecimal.valueOf(2520), 1000000L, null, null, null, null, null);

        given(stockPricePort.loadPricesByTickers(anyList(), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(Map.of("AAPL", List.of(aaplPrice)));
        given(loadBenchmarkPort.loadBenchmarkPrices(anyString(), any(LocalDate.class), any(LocalDate.class))).willReturn(List.of(benchmarkPrice));

        // when
        SimulationData data = simulationDataProvider.loadData(symbols, benchmarkTickers, start, end);

        // then
        assertThat(data.stockPrices().get("AAPL")).hasSize(1);
        assertThat(data.benchmarkPrices()).containsKey("KOSPI");
        assertThat(data.benchmarkPrices().get("KOSPI").get(0).baseDate()).isEqualTo(start);
    }

    @Test
    @DisplayName("벤치마크 티커가 없으면 주가 데이터만 로딩한다")
    void load_simulation_data_without_benchmarks() {
        List<String> symbols = List.of("AAPL");
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);

        StockPriceResult aaplPrice = new StockPriceResult(start, BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102), BigDecimal.valueOf(102), 1000L, null, null, null, null, null);

        given(stockPricePort.loadPricesByTickers(anyList(), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(Map.of("AAPL", List.of(aaplPrice)));

        SimulationData data = simulationDataProvider.loadData(symbols, null, start, end);

        assertThat(data.stockPrices().get("AAPL")).hasSize(1);
        assertThat(data.benchmarkPrices()).isEmpty();
    }
}

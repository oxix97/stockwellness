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
        String benchmark = "KOSPI";
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(2);

        StockPriceResult aaplPrice = new StockPriceResult(start, BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102), BigDecimal.valueOf(102), 1000L);
        StockPriceResult benchmarkPrice = new StockPriceResult(start, BigDecimal.valueOf(2500), BigDecimal.valueOf(2550), BigDecimal.valueOf(2450), BigDecimal.valueOf(2520), BigDecimal.valueOf(2520), 1000000L);

        given(stockPricePort.loadPricesByTickers(symbols, start, end))
                .willReturn(Map.of("AAPL", List.of(aaplPrice)));
        given(loadBenchmarkPort.loadBenchmarkPrices("KOSPI", start, end)).willReturn(List.of(benchmarkPrice));

        // when
        SimulationData data = simulationDataProvider.loadData(symbols, benchmark, start, end);

        // then
        assertThat(data.stockPrices().get("AAPL")).hasSize(1);
        assertThat(data.benchmarkPrices()).hasSize(1);
        assertThat(data.benchmarkPrices().get(0).baseDate()).isEqualTo(start);
    }
}

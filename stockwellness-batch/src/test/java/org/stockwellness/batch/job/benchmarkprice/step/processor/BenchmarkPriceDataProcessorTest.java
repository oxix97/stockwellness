package org.stockwellness.batch.job.benchmarkprice.step.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.benchmarkprice.BenchmarkPriceSyncService;
import org.stockwellness.application.port.in.batch.BenchmarkPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("BenchmarkPriceDataProcessor 단위 테스트")
class BenchmarkPriceDataProcessorTest {

    private BenchmarkPricePort benchmarkPricePort;
    private BenchmarkPriceSyncService benchmarkPriceSyncService;

    @BeforeEach
    void setUp() {
        benchmarkPricePort = mock(BenchmarkPricePort.class);
        benchmarkPriceSyncService = new BenchmarkPriceSyncService(benchmarkPricePort);
    }

    @Test
    @DisplayName("해외 지수(S&P 500)인 경우 전일 종가를 기반으로 수동 계산한다")
    void process_overseasIndex_calculatesManually() {
        BenchmarkType type = BenchmarkType.S_P_500;
        LocalDate today = LocalDate.of(2026, 4, 2);

        BenchmarkPrice prevPrice = mock(BenchmarkPrice.class);
        given(prevPrice.getClosePrice()).willReturn(BigDecimal.valueOf(5000));
        given(benchmarkPricePort.findLatestBefore(eq(type.getTicker()), eq(today)))
                .willReturn(Optional.of(prevPrice));

        BenchmarkPrice result = benchmarkPriceSyncService.toBenchmarkPrice(
                new BenchmarkPriceSyncUseCase.BenchmarkPriceCommand(
                        type,
                        today,
                        BigDecimal.valueOf(5000),
                        BigDecimal.valueOf(5100),
                        BigDecimal.valueOf(4980),
                        BigDecimal.valueOf(5050),
                        BigDecimal.ZERO,
                        10000L
                )
        ).benchmarkPrice();

        assertThat(result).isNotNull();
        assertThat(result.getChangeRate()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("국내 지수(KOSPI)의 API 등락률이 0인 경우 전일 종가를 기반으로 수동 계산한다")
    void process_domesticIndexWithZeroChangeRate_calculatesManually() {
        BenchmarkType type = BenchmarkType.KOSPI;
        LocalDate today = LocalDate.of(2026, 4, 2);

        BenchmarkPrice prevPrice = mock(BenchmarkPrice.class);
        given(prevPrice.getClosePrice()).willReturn(BigDecimal.valueOf(2500));
        given(benchmarkPricePort.findLatestBefore(eq(type.getTicker()), eq(today)))
                .willReturn(Optional.of(prevPrice));

        BenchmarkPrice result = benchmarkPriceSyncService.toBenchmarkPrice(
                new BenchmarkPriceSyncUseCase.BenchmarkPriceCommand(
                        type,
                        today,
                        BigDecimal.valueOf(2500),
                        BigDecimal.valueOf(2550),
                        BigDecimal.valueOf(2480),
                        BigDecimal.valueOf(2525),
                        BigDecimal.ZERO,
                        10000L
                )
        ).benchmarkPrice();

        assertThat(result).isNotNull();
        assertThat(result.getChangeRate()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("국내 지수의 API 등락률이 정상적인 경우 수동 계산을 건너뛰고 API 값을 신뢰한다")
    void process_domesticIndexWithValidChangeRate_usesApiValue() {
        BenchmarkType type = BenchmarkType.KOSPI;
        LocalDate today = LocalDate.of(2026, 4, 2);

        BenchmarkPrice result = benchmarkPriceSyncService.toBenchmarkPrice(
                new BenchmarkPriceSyncUseCase.BenchmarkPriceCommand(
                        type,
                        today,
                        BigDecimal.valueOf(2500),
                        BigDecimal.valueOf(2550),
                        BigDecimal.valueOf(2480),
                        BigDecimal.valueOf(2525),
                        new BigDecimal("1.5"),
                        10000L
                )
        ).benchmarkPrice();

        assertThat(result).isNotNull();
        assertThat(result.getChangeRate()).isEqualByComparingTo("1.5");
    }
}

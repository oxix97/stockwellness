package org.stockwellness.application.service.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.batch.BenchmarkPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BenchmarkPriceSyncService 단위 테스트")
class BenchmarkPriceSyncServiceTest {

    @Mock
    private BenchmarkPricePort benchmarkPricePort;

    @Test
    @DisplayName("등락률이 없으면 이전 종가를 조회해 등락률을 계산한다")
    void toBenchmarkPrice_CalculatesChangeRateFromPreviousClose() {
        BenchmarkPriceSyncService service = new BenchmarkPriceSyncService(benchmarkPricePort);
        LocalDate baseDate = LocalDate.of(2026, 4, 24);
        BenchmarkPrice previous = BenchmarkPrice.of("코스피 200", "2001", baseDate.minusDays(1), BigDecimal.valueOf(100));
        given(benchmarkPricePort.findLatestBefore("2001", baseDate)).willReturn(Optional.of(previous));

        BenchmarkPriceSyncUseCase.BenchmarkPriceResult result = service.toBenchmarkPrice(new BenchmarkPriceSyncUseCase.BenchmarkPriceCommand(
                BenchmarkType.KOSPI_200,
                baseDate,
                BigDecimal.valueOf(101),
                BigDecimal.valueOf(106),
                BigDecimal.valueOf(99),
                BigDecimal.valueOf(105),
                null,
                1_000L
        ));

        assertThat(result.benchmarkPrice().getTicker()).isEqualTo("2001");
        assertThat(result.benchmarkPrice().getClosePrice()).isEqualByComparingTo("105");
        assertThat(result.benchmarkPrice().getChangeRate()).isEqualByComparingTo("5.000000");
    }

    @Test
    @DisplayName("이전 종가 캐시가 있으면 포트를 다시 조회하지 않는다")
    void toBenchmarkPrice_UsesCachedPreviousClose() {
        BenchmarkPriceSyncService service = new BenchmarkPriceSyncService(benchmarkPricePort);
        LocalDate firstDate = LocalDate.of(2026, 4, 23);
        LocalDate secondDate = LocalDate.of(2026, 4, 24);
        BenchmarkPrice previous = BenchmarkPrice.of("코스피 200", "2001", firstDate.minusDays(1), BigDecimal.valueOf(100));
        given(benchmarkPricePort.findLatestBefore("2001", firstDate)).willReturn(Optional.of(previous));

        service.toBenchmarkPrice(command(BenchmarkType.KOSPI_200, firstDate, BigDecimal.valueOf(105), null));
        BenchmarkPriceSyncUseCase.BenchmarkPriceResult second = service.toBenchmarkPrice(
                command(BenchmarkType.KOSPI_200, secondDate, BigDecimal.valueOf(110), null)
        );

        assertThat(second.benchmarkPrice().getChangeRate()).isEqualByComparingTo("4.761900");
        verify(benchmarkPricePort, times(1)).findLatestBefore("2001", firstDate);
    }

    @Test
    @DisplayName("이전 종가가 없으면 등락률을 0으로 저장한다")
    void toBenchmarkPrice_DefaultsChangeRateToZeroWhenPreviousCloseMissing() {
        BenchmarkPriceSyncService service = new BenchmarkPriceSyncService(benchmarkPricePort);
        LocalDate baseDate = LocalDate.of(2026, 4, 24);
        given(benchmarkPricePort.findLatestBefore("2001", baseDate)).willReturn(Optional.empty());

        BenchmarkPriceSyncUseCase.BenchmarkPriceResult result = service.toBenchmarkPrice(
                command(BenchmarkType.KOSPI_200, baseDate, BigDecimal.valueOf(105), null)
        );

        assertThat(result.benchmarkPrice().getChangeRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("국내 지수의 유효한 등락률은 재계산하지 않고 그대로 사용한다")
    void toBenchmarkPrice_KeepsProvidedDomesticChangeRate() {
        BenchmarkPriceSyncService service = new BenchmarkPriceSyncService(benchmarkPricePort);
        LocalDate baseDate = LocalDate.of(2026, 4, 24);

        BenchmarkPriceSyncUseCase.BenchmarkPriceResult result = service.toBenchmarkPrice(
                command(BenchmarkType.KOSPI_200, baseDate, BigDecimal.valueOf(105), BigDecimal.valueOf(1.25))
        );

        assertThat(result.benchmarkPrice().getChangeRate()).isEqualByComparingTo("1.25");
    }

    private BenchmarkPriceSyncUseCase.BenchmarkPriceCommand command(
            BenchmarkType type,
            LocalDate baseDate,
            BigDecimal closePrice,
            BigDecimal changeRate
    ) {
        return new BenchmarkPriceSyncUseCase.BenchmarkPriceCommand(
                type,
                baseDate,
                closePrice.subtract(BigDecimal.ONE),
                closePrice.add(BigDecimal.ONE),
                closePrice.subtract(BigDecimal.TEN),
                closePrice,
                changeRate,
                1_000L
        );
    }
}

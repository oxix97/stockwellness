package org.stockwellness.application.service.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.batch.BenchmarkPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.price.BenchmarkPrice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BenchmarkPriceSyncService 단위 테스트")
class BenchmarkPriceSyncServiceTest {

    @InjectMocks
    private BenchmarkPriceSyncService benchmarkPriceSyncService;

    @Mock
    private BenchmarkPricePort benchmarkPricePort;

    @Nested
    @DisplayName("지수 가격 변환(toBenchmarkPrice) 테스트")
    class ToBenchmarkPriceCases {

        @Test
        @DisplayName("등락률이 없으면 이전 종가를 조회해 등락률을 계산한다")
        void shouldCalculateChangeRateWhenNotProvided() {
            // given
            LocalDate baseDate = LocalDate.of(2026, 4, 24);
            BenchmarkPrice previous = BenchmarkPrice.of("코스피 200", "2001", baseDate.minusDays(1), BigDecimal.valueOf(100));
            given(benchmarkPricePort.findLatestBefore("2001", baseDate)).willReturn(Optional.of(previous));

            BenchmarkPriceSyncUseCase.BenchmarkPriceCommand command = createCommand(
                    BenchmarkType.KOSPI_200, baseDate, BigDecimal.valueOf(105), null
            );

            // when
            BenchmarkPriceSyncUseCase.BenchmarkPriceResult result = benchmarkPriceSyncService.toBenchmarkPrice(command);

            // then
            assertThat(result.benchmarkPrice().getTicker()).isEqualTo("2001");
            assertThat(result.benchmarkPrice().getChangeRate()).isEqualByComparingTo("5.000000");
            verify(benchmarkPricePort).findLatestBefore("2001", baseDate);
        }

        @Test
        @DisplayName("이전 종가 캐시가 있으면 포트를 다시 조회하지 않고 재사용한다")
        void shouldReuseCachedPreviousClose() {
            // given
            LocalDate firstDate = LocalDate.of(2026, 4, 23);
            LocalDate secondDate = LocalDate.of(2026, 4, 24);
            BenchmarkPrice previous = BenchmarkPrice.of("코스피 200", "2001", firstDate.minusDays(1), BigDecimal.valueOf(100));
            given(benchmarkPricePort.findLatestBefore("2001", firstDate)).willReturn(Optional.of(previous));

            // when
            benchmarkPriceSyncService.toBenchmarkPrice(createCommand(BenchmarkType.KOSPI_200, firstDate, BigDecimal.valueOf(105), null));
            BenchmarkPriceSyncUseCase.BenchmarkPriceResult result = benchmarkPriceSyncService.toBenchmarkPrice(
                    createCommand(BenchmarkType.KOSPI_200, secondDate, BigDecimal.valueOf(110), null)
            );

            // then
            assertThat(result.benchmarkPrice().getChangeRate()).isEqualByComparingTo("4.761900");
            verify(benchmarkPricePort, times(1)).findLatestBefore(anyString(), any());
        }

        @Test
        @DisplayName("이전 종가가 없으면 등락률을 0으로 설정한다")
        void shouldDefaultChangeRateToZeroWhenNoPreviousClose() {
            // given
            LocalDate baseDate = LocalDate.of(2026, 4, 24);
            given(benchmarkPricePort.findLatestBefore("2001", baseDate)).willReturn(Optional.empty());

            // when
            BenchmarkPriceSyncUseCase.BenchmarkPriceResult result = benchmarkPriceSyncService.toBenchmarkPrice(
                    createCommand(BenchmarkType.KOSPI_200, baseDate, BigDecimal.valueOf(105), null)
            );

            // then
            assertThat(result.benchmarkPrice().getChangeRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("등락률이 제공되면 재계산하지 않고 그대로 사용한다")
        void shouldKeepProvidedChangeRate() {
            // given
            LocalDate baseDate = LocalDate.of(2026, 4, 24);
            BigDecimal providedRate = BigDecimal.valueOf(1.25);

            // when
            BenchmarkPriceSyncUseCase.BenchmarkPriceResult result = benchmarkPriceSyncService.toBenchmarkPrice(
                    createCommand(BenchmarkType.KOSPI_200, baseDate, BigDecimal.valueOf(105), providedRate)
            );

            // then
            assertThat(result.benchmarkPrice().getChangeRate()).isEqualByComparingTo(providedRate);
            verify(benchmarkPricePort, never()).findLatestBefore(anyString(), any());
        }
    }

    private BenchmarkPriceSyncUseCase.BenchmarkPriceCommand createCommand(
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

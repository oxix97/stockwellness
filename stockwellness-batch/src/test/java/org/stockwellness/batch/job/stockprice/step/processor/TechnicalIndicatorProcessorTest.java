package org.stockwellness.batch.job.stockprice.step.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.batch.stockprice.step.processor.TechnicalIndicatorProcessor;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("TechnicalIndicatorProcessor 테스트")
class TechnicalIndicatorProcessorTest {

    @Mock
    private StockPricePort stockPricePort;

    @InjectMocks
    private TechnicalIndicatorProcessor technicalIndicatorProcessor;

    @Test
    @DisplayName("최근 시세가 5건 미만이면 null을 반환한다")
    void process_returnsNullWhenPriceCountIsInsufficient() {
        Stock stock = createStock("005930");
        given(stockPricePort.findRecentPricesWithDateByStock(eq(stock), any(LocalDate.class), eq(120)))
                .willReturn(new ArrayList<>(List.of(
                        createPrice(stock, LocalDate.of(2026, 4, 18), "100"),
                        createPrice(stock, LocalDate.of(2026, 4, 17), "101"),
                        createPrice(stock, LocalDate.of(2026, 4, 16), "102"),
                        createPrice(stock, LocalDate.of(2026, 4, 15), "103")
                )));

        StockPrice result = technicalIndicatorProcessor.process(stock);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("최근 시세가 충분하면 최신 1건에 기술지표를 업데이트한다")
    void process_updatesLatestPriceIndicators() {
        Stock stock = createStock("005930");
        StockPrice latestPrice = createPrice(stock, LocalDate.of(2026, 4, 18), "104");
        given(stockPricePort.findRecentPricesWithDateByStock(eq(stock), any(LocalDate.class), eq(120)))
                .willReturn(new ArrayList<>(List.of(
                        latestPrice,
                        createPrice(stock, LocalDate.of(2026, 4, 17), "103"),
                        createPrice(stock, LocalDate.of(2026, 4, 16), "102"),
                        createPrice(stock, LocalDate.of(2026, 4, 15), "101"),
                        createPrice(stock, LocalDate.of(2026, 4, 14), "100")
                )));

        StockPrice result = technicalIndicatorProcessor.process(stock);

        assertThat(result).isSameAs(latestPrice);
        assertThat(result.getIndicators()).isNotNull();
        assertThat(result.getIndicators().getMa5()).isEqualByComparingTo("102");
    }

    @Test
    @DisplayName("역순으로 조회된 가격도 계산 전에 날짜 오름차순으로 정렬한다")
    void process_sortsPricesAscendingBeforeCalculation() {
        Stock stock = createStock("005930");
        StockPrice latestPrice = createPrice(stock, LocalDate.of(2026, 4, 18), "50");
        given(stockPricePort.findRecentPricesWithDateByStock(eq(stock), any(LocalDate.class), eq(120)))
                .willReturn(new ArrayList<>(List.of(
                        latestPrice,
                        createPrice(stock, LocalDate.of(2026, 4, 17), "40"),
                        createPrice(stock, LocalDate.of(2026, 4, 16), "30"),
                        createPrice(stock, LocalDate.of(2026, 4, 15), "20"),
                        createPrice(stock, LocalDate.of(2026, 4, 14), "10")
                )));

        StockPrice result = technicalIndicatorProcessor.process(stock);

        assertThat(result).isSameAs(latestPrice);
        assertThat(result.getIndicators()).isNotNull();
        assertThat(result.getIndicators().getMa5()).isEqualByComparingTo("30");
    }

    private Stock createStock(String ticker) {
        return Stock.of(ticker, "KR" + ticker, "테스트", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
    }

    private StockPrice createPrice(Stock stock, LocalDate baseDate, String closePrice) {
        BigDecimal price = new BigDecimal(closePrice);
        return StockPrice.of(
                stock,
                baseDate,
                price,
                price,
                price,
                price,
                price,
                price,
                100L,
                price.multiply(BigDecimal.valueOf(1000L)),
                null
        );
    }
}

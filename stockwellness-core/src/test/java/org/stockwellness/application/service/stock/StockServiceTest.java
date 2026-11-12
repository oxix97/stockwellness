package org.stockwellness.application.service.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.event.StockSearchEvent;
import org.stockwellness.domain.stock.exception.StockPriceException;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.PriceBasis;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.stockwellness.global.error.ErrorCode.STOCK_NOT_FOUND;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 단위 테스트")
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockPriceReadPort stockPriceReadPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("종목 상세 조회 시 시세 정보가 있으면 변동률이 정확히 계산된다")
    void getStockDetail_with_price() {
        // Given
        String ticker = "005930";
        StockSector sector = StockSector.of(null, null, null, "Information Technology");
        Stock stock = Stock.of(ticker, "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, sector, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 1L);

        given(stockRepository.findByTicker(ticker)).willReturn(Optional.of(stock));
        given(stockPriceReadPort.recent(Set.of(1L), "KRX", LocalDate.now(), 2, PriceBasis.RAW))
                .willReturn(Map.of(1L, List.of(
                        observation(1L, LocalDate.now(), "80000"),
                        observation(1L, LocalDate.now().minusDays(1), "78000"))));

        // When
        StockDetailResult result = stockService.getStockDetail(ticker);

        // Then
        assertThat(result.ticker()).isEqualTo(ticker);
        assertThat(result.currentPrice()).isEqualByComparingTo(BigDecimal.valueOf(80000));
        assertThat(result.priceChange()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(result.fluctuationRate()).isEqualByComparingTo(new BigDecimal("2.5600")); // (2000/78000) -> 0.0256 * 100
        assertThat(result.rsi14()).isNull();
        assertThat(result.aiInsight()).isEqualTo("기술적 지표 집계 중입니다.");
    }

    @Test
    @DisplayName("종목 상세 조회 시 시세 정보가 없으면 가격 필드는 null과 안전한 안내 문구로 반환한다")
    void getStockDetail_without_price() {
        // Given
        String ticker = "005930";
        StockSector sector = StockSector.of(null, null, null, "Information Technology");
        Stock stock = Stock.of(ticker, "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, sector, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 1L);

        given(stockRepository.findByTicker(ticker)).willReturn(Optional.of(stock));
        given(stockPriceReadPort.recent(Set.of(1L), "KRX", LocalDate.now(), 2, PriceBasis.RAW))
                .willReturn(Map.of(1L, List.of()));

        // When
        StockDetailResult result = stockService.getStockDetail(ticker);

        // Then
        assertThat(result.baseDate()).isNull();
        assertThat(result.currentPrice()).isNull();
        assertThat(result.priceChange()).isNull();
        assertThat(result.fluctuationRate()).isNull();
        assertThat(result.openPrice()).isNull();
        assertThat(result.highPrice()).isNull();
        assertThat(result.lowPrice()).isNull();
        assertThat(result.volume()).isNull();
        assertThat(result.tradingValue()).isNull();
        assertThat(result.marketCap()).isNull();
        assertThat(result.aiInsight()).isEqualTo("시세 데이터가 없어 분석할 수 없습니다.");
    }

    @Test
    @DisplayName("이전 거래일 가격이 없으면 변동률을 0으로 만들지 않는다")
    void getStockDetail_without_previous_price() {
        String ticker = "005930";
        StockSector sector = StockSector.of(null, null, null, "Information Technology");
        Stock stock = Stock.of(ticker, "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, sector, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(stockRepository.findByTicker(ticker)).willReturn(Optional.of(stock));
        given(stockPriceReadPort.recent(Set.of(1L), "KRX", LocalDate.now(), 2, PriceBasis.RAW))
                .willReturn(Map.of(1L, List.of(observation(1L, LocalDate.now(), "80000"))));

        StockDetailResult result = stockService.getStockDetail(ticker);

        assertThat(result.currentPrice()).isEqualByComparingTo("80000");
        assertThat(result.priceChange()).isNull();
        assertThat(result.fluctuationRate()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 종목 상세 조회는 S001 표준 예외를 반환한다")
    void getStockDetail_unknown_ticker_throws_stock_not_found() {
        String ticker = "999999";
        given(stockRepository.findByTicker(ticker)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.getStockDetail(ticker))
                .isInstanceOf(StockPriceException.class)
                .extracting(exception -> ((StockPriceException) exception).getErrorCode())
                .isEqualTo(STOCK_NOT_FOUND);
    }

    @Test
    @DisplayName("종목 검색 시 검색 이벤트가 발행된다")
    void searchStocks_publishes_event() {
        // Given
        SearchStockQuery query = new SearchStockQuery("삼성", null, null, null, null, 0, 10);
        given(stockRepository.searchByCondition(any(), any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of()));

        // When
        Slice<StockSearchResult> result = stockService.searchStocks(query);

        // Then
        verify(eventPublisher).publishEvent(any(StockSearchEvent.class));
    }

    private static PriceObservation observation(Long stockId, LocalDate date, String close) {
        BigDecimal value = new BigDecimal(close);
        return new PriceObservation(stockId, date, "KRX", PriceBasis.RAW, value, value, value, value,
                100L, BigDecimal.TEN, "FINAL", 1);
    }
}

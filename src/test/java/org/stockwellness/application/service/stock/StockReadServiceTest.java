package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.stockwellness.application.port.in.stock.query.SearchStockQuery;
import org.stockwellness.application.port.in.stock.result.StockDetailResult;
import org.stockwellness.application.port.in.stock.result.StockSearchResult;
import org.stockwellness.application.port.out.stock.LoadStockHistoryPort;
import org.stockwellness.application.port.out.stock.LoadStockPort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.fixture.StockFixture;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Stock 조회 서비스 단위 테스트")
class StockReadServiceTest {

    @InjectMocks
    private StockReadService stockReadService;

    @Mock
    private LoadStockPort loadStockPort;

    @Mock
    private LoadStockHistoryPort loadStockHistoryPort;

    @Nested
    @DisplayName("종목 검색 (Search)")
    class Search {

        @Test
        @DisplayName("성공: 검색 조건에 맞는 종목 리스트를 반환한다")
        void search_success() {
            // given
            SearchStockQuery query = StockFixture.createSearchQuery("삼성");
            Stock stock = StockFixture.createStock();
            given(loadStockPort.searchStocks(query))
                    .willReturn(new SliceImpl<>(List.of(stock)));

            // when
            Slice<StockSearchResult> results = stockReadService.searchStocks(query);

            // then
            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getContent().get(0).isinCode()).isEqualTo(StockFixture.ISIN_CODE);
        }
    }

    @Nested
    @DisplayName("종목 상세 조회 (Detail)")
    class Detail {

        @Test
        @DisplayName("성공: 티커로 종목 상세 정보를 조회한다")
        void detail_success() {
            // given
            String ticker = "005930";
            Stock stock = StockFixture.createStock();
            StockHistory history = StockFixture.createHistory(StockFixture.ISIN_CODE, java.time.LocalDate.now(), 70000);

            given(loadStockPort.loadStockByTicker(ticker)).willReturn(Optional.of(stock));
            given(loadStockHistoryPort.findLatestHistory(StockFixture.ISIN_CODE)).willReturn(Optional.of(history));

            // when
            StockDetailResult result = stockReadService.getStockDetail(ticker);

            // then
            assertThat(result.isinCode()).isEqualTo(StockFixture.ISIN_CODE);
            assertThat(result.closePrice()).isEqualByComparingTo("70000");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 티커로 조회하면 예외가 발생한다")
        void detail_fail_not_found() {
            // given
            String invalidTicker = "INVALID";
            given(loadStockPort.loadStockByTicker(invalidTicker)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> stockReadService.getStockDetail(invalidTicker))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 종목");
        }
    }
}
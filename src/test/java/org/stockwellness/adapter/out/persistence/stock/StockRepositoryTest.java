package org.stockwellness.adapter.out.persistence.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.config.TestJpaConfig;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.SectorCode;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.fixture.StockFixture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, TestJpaConfig.class})
class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @Nested
    @DisplayName("종목 통합 검색 성공 테스트")
    class SearchSuccessTest {

        @Test
        @DisplayName("키워드가 종목명의 어느 위치에 있든 검색되며, 정확도가 높은 순서대로 정렬된다")
        void searchByNameAccuracy() {
            // given
            stockRepository.save(StockFixture.create("005930", "삼성전자", MarketType.KOSPI));
            stockRepository.save(StockFixture.create("000001", "삼성", MarketType.KOSPI));
            stockRepository.save(StockFixture.create("000002", "에이삼성", MarketType.KOSPI));

            PageRequest pageable = PageRequest.of(0, 10);

            // when
            Slice<Stock> result = stockRepository.searchByCondition("삼성", null, null, pageable);

            // then
            List<Stock> content = result.getContent();
            assertThat(content).hasSize(3);
            assertThat(content.get(0).getName()).isEqualTo("삼성");
            assertThat(content.get(1).getName()).isEqualTo("삼성전자");
            assertThat(content.get(2).getName()).isEqualTo("에이삼성");
        }

        @Test
        @DisplayName("티커 완전 일치가 종목명 부분 일치보다 우선순위가 높다")
        void searchByTickerPriority() {
            // given
            stockRepository.save(StockFixture.create("AAPL", "Apple Inc", MarketType.NASDAQ));
            stockRepository.save(StockFixture.create("APP", "Application Tool", MarketType.NASDAQ));

            PageRequest pageable = PageRequest.of(0, 10);

            // when
            Slice<Stock> result = stockRepository.searchByCondition("APP", null, null, pageable);

            // then
            List<Stock> content = result.getContent();
            assertThat(content.get(0).getTicker()).isEqualTo("APP");
            assertThat(content.get(1).getName()).isEqualTo("Apple Inc");
        }

        @Test
        @DisplayName("영문 종목명 및 티커 검색 시 대소문자를 구분하지 않는다")
        void searchWithCaseInsensitivity() {
            // given
            stockRepository.save(StockFixture.create("AAPL", "Apple Inc", MarketType.NASDAQ));

            PageRequest pageable = PageRequest.of(0, 10);

            // when & then
            assertThat(stockRepository.searchByCondition("aapl", null, null, pageable).getContent()).hasSize(1);
            assertThat(stockRepository.searchByCondition("APPLE", null, null, pageable).getContent()).hasSize(1);
        }

        @Test
        @DisplayName("키워드와 여러 필터(마켓, 상태)를 동시에 적용하여 검색할 수 있다")
        void searchWithMultipleFilters() {
            // given
            stockRepository.save(StockFixture.create("005930", "삼성전자", MarketType.KOSPI));
            stockRepository.save(Stock.of("005935", "STD_005935", "삼성전자우", MarketType.KOSPI, 
                    org.stockwellness.domain.stock.Currency.KRW, "009", "전기전자", StockStatus.HALTED));

            PageRequest pageable = PageRequest.of(0, 10);

            // when
            Slice<Stock> result = stockRepository.searchByCondition("삼성", MarketType.KOSPI, StockStatus.ACTIVE, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTicker()).isEqualTo("005930");
        }

        @Test
        @DisplayName("특정 섹터에 속한 종목들만 조회할 수 있다")
        void searchBySector() {
            // given
            SectorCode food = SectorCode.FOOD_BEVERAGE;
            stockRepository.save(StockFixture.createWithSector("005930", "삼성전자", MarketType.KOSPI, food));
            stockRepository.save(StockFixture.createWithSector("AAPL", "Apple Inc", MarketType.NASDAQ, SectorCode.PHARMACEUTICAL));

            // when
            List<Stock> result = stockRepository.findBySectorCode(food.getCode());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("삼성전자");
            assertThat(result.get(0).getSectorCode()).isEqualTo(food.getCode());
        }

        @Test
        @DisplayName("신규 상장 종목을 최근 등록순으로 조회할 수 있다")
        void searchNewListings() {
            // given
            stockRepository.save(StockFixture.create("T1", "Stock1", MarketType.KOSPI));
            stockRepository.save(StockFixture.create("T2", "Stock2", MarketType.KOSPI));

            // when
            List<Stock> result = stockRepository.findTop10ByOrderByCreatedAtDesc();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTicker()).isEqualTo("T2");
            assertThat(result.get(1).getTicker()).isEqualTo("T1");
        }
    }

    @Nested
    @DisplayName("종목 통합 검색 실패 및 엣지 케이스 테스트")
    class SearchFailureAndEdgeTest {

        @Test
        @DisplayName("일치하는 종목이 없는 키워드로 검색하면 빈 리스트를 반환한다")
        void searchWithNoMatchKeyword() {
            // given
            stockRepository.save(StockFixture.createSamsung());
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            Slice<Stock> result = stockRepository.searchByCondition("존재하지않는종목", null, null, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("키워드가 null인 경우 검색 조건 없이 빈 결과를 반환하거나 전체 조회를 수행하지 않는다")
        void searchWithNullKeyword() {
            // given
            stockRepository.save(StockFixture.createSamsung());
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            Slice<Stock> result = stockRepository.searchByCondition(null, null, null, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("공백 문자열로 검색 시 빈 결과를 반환한다")
        void searchWithBlankKeyword() {
            // given
            stockRepository.save(StockFixture.createSamsung());
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            Slice<Stock> result = stockRepository.searchByCondition("   ", null, null, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("마켓 필터가 데이터와 일치하지 않으면 결과가 반환되지 않는다")
        void searchWithMismatchedMarket() {
            // given
            stockRepository.save(StockFixture.createSamsung()); // KOSPI
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            Slice<Stock> result = stockRepository.searchByCondition("삼성", MarketType.NASDAQ, null, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("매우 짧은 키워드(1글자) 검색도 정상적으로 동작해야 한다")
        void searchWithShortKeyword() {
            // given
            stockRepository.save(StockFixture.createSamsung()); // 삼성전자
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            Slice<Stock> result = stockRepository.searchByCondition("삼", null, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("숫자로 구성된 키워드가 티커의 앞부분과 일치하면 검색된다")
        void searchWithNumericalTickerPrefix() {
            // given
            stockRepository.save(StockFixture.createSamsung()); // 005930
            PageRequest pageable = PageRequest.of(0, 10);

            // when
            Slice<Stock> result = stockRepository.searchByCondition("0059", null, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }
}

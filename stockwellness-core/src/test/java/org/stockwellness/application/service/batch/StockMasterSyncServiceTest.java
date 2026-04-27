package org.stockwellness.application.service.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.domain.stock.*;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockMasterSyncService 단위 테스트")
class StockMasterSyncServiceTest {

    @InjectMocks
    private StockMasterSyncService stockMasterSyncService;

    @Mock
    private KisMasterClient kisMasterClient;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private MarketIndexRepository marketIndexRepository;

    @Nested
    @DisplayName("KOSPI 종목 업서트(upsertKospi) 테스트")
    class UpsertKospiCases {

        @Test
        @DisplayName("신규 종목이면 Stock을 생성하여 반환한다")
        void shouldCreateNewStockWhenTickerDoesNotExist() {
            // given
            KospiItem item = createKospiItem("005930", "삼성전자", "0029");
            given(marketIndexRepository.findActiveIndexMap()).willReturn(Map.of("0029", MarketIndex.of("0029", "전기전자")));
            given(stockRepository.findByTicker("005930")).willReturn(Optional.empty());

            // when
            StockMasterSyncUseCase.StockMasterSyncResult result = stockMasterSyncService.upsertKospi(
                    new StockMasterSyncUseCase.KospiMasterSyncCommand(item)
            );

            // then
            assertThat(result.created()).isTrue();
            assertThat(result.stock().getTicker()).isEqualTo("005930");
            assertThat(result.stock().getName()).isEqualTo("삼성전자");
            assertThat(result.stock().getMarketType()).isEqualTo(MarketType.KOSPI);
            assertThat(result.stock().getSector().getSectorName()).isEqualTo("전기전자");
        }

        @Test
        @DisplayName("기존 종목이면 정보를 업데이트한다")
        void shouldUpdateExistingStock() {
            // given
            Stock existing = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW,
                    StockSector.of("0001", "0029", null, "기존업종"), StockStatus.ACTIVE);
            KospiItem item = createKospiItem("005930", "삼성전자우", "0029");
            
            given(marketIndexRepository.findActiveIndexMap()).willReturn(Map.of("0029", MarketIndex.of("0029", "전기전자")));
            given(stockRepository.findByTicker("005930")).willReturn(Optional.of(existing));

            // when
            StockMasterSyncUseCase.StockMasterSyncResult result = stockMasterSyncService.upsertKospi(
                    new StockMasterSyncUseCase.KospiMasterSyncCommand(item)
            );

            // then
            assertThat(result.created()).isFalse();
            assertThat(result.stock().getName()).isEqualTo("삼성전자우");
            assertThat(result.stock().getSector().getSectorName()).isEqualTo("전기전자");
        }

        @Test
        @DisplayName("업종 맵에 코드가 없으면 기본값을 사용한다")
        void shouldFallbackToDefaultWhenSectorCodeMissingInMap() {
            // given
            KospiItem item = createKospiItem("005930", "삼성전자", "9999");
            given(marketIndexRepository.findActiveIndexMap()).willReturn(Map.of()); // 빈 맵
            given(stockRepository.findByTicker("005930")).willReturn(Optional.empty());

            // when
            StockMasterSyncUseCase.StockMasterSyncResult result = stockMasterSyncService.upsertKospi(
                    new StockMasterSyncUseCase.KospiMasterSyncCommand(item)
            );

            // then
            assertThat(result.stock().getSector().getSectorName()).isEqualTo("0001"); // large sector code가 기본값으로 사용됨
        }

        @Test
        @DisplayName("단축코드가 공백이면 동기화를 스킵한다")
        void shouldSkipWhenShortCodeIsBlank() {
            // given
            KospiItem item = mock(KospiItem.class);
            given(item.shortCode()).willReturn(" ");
            given(item.isinCode()).willReturn("KR7000000000");

            // when
            StockMasterSyncUseCase.StockMasterSyncResult result = stockMasterSyncService.upsertKospi(
                    new StockMasterSyncUseCase.KospiMasterSyncCommand(item)
            );

            // then
            assertThat(result.stock()).isNull();
            verifyNoInteractions(marketIndexRepository, stockRepository);
        }
    }

    @Nested
    @DisplayName("KOSDAQ 종목 업서트(upsertKosdaq) 테스트")
    class UpsertKosdaqCases {
        @Test
        @DisplayName("신규 KOSDAQ 종목을 정상적으로 생성한다")
        void shouldCreateNewKosdaqStock() {
            // given
            KosdaqItem item = createKosdaqItem("000250", "삼천당제약", "0027");
            given(marketIndexRepository.findActiveIndexMap()).willReturn(Map.of("0027", MarketIndex.of("0027", "제약")));
            given(stockRepository.findByTicker("000250")).willReturn(Optional.empty());

            // when
            StockMasterSyncUseCase.StockMasterSyncResult result = stockMasterSyncService.upsertKosdaq(
                    new StockMasterSyncUseCase.KosdaqMasterSyncCommand(item)
            );

            // then
            assertThat(result.created()).isTrue();
            assertThat(result.stock().getMarketType()).isEqualTo(MarketType.KOSDAQ);
            assertThat(result.stock().getSector().getSectorName()).isEqualTo("제약");
        }
    }

    @Nested
    @DisplayName("상장폐지 처리(markDelisted) 테스트")
    class DelistCases {

        @Test
        @DisplayName("활성 Ticker 목록이 비어 있으면 스킵한다")
        void shouldSkipWhenActiveTickersIsEmpty() {
            // when
            StockMasterSyncUseCase.StockDelistResult result = stockMasterSyncService.markDelisted(
                    new StockMasterSyncUseCase.StockDelistCommand(MarketType.KOSPI, Set.of())
            );

            // then
            assertThat(result.delistedCount()).isZero();
            verify(stockRepository, never()).delistMissingStocks(any(), any());
        }

        @Test
        @DisplayName("활성 Ticker 목록에 없는 종목들을 상장폐지한다")
        void shouldMarkMissingStocksAsDelisted() {
            // given
            Set<String> activeTickers = Set.of("005930", "000660");
            given(stockRepository.delistMissingStocks(MarketType.KOSPI, activeTickers)).willReturn(5);

            // when
            StockMasterSyncUseCase.StockDelistResult result = stockMasterSyncService.markDelisted(
                    new StockMasterSyncUseCase.StockDelistCommand(MarketType.KOSPI, activeTickers)
            );

            // then
            assertThat(result.delistedCount()).isEqualTo(5);
            verify(stockRepository).delistMissingStocks(MarketType.KOSPI, activeTickers);
        }
    }

    private KospiItem createKospiItem(String shortCode, String koreanName, String sectorMedium) {
        KospiItem item = mock(KospiItem.class);
        given(item.shortCode()).willReturn(shortCode);
        given(item.isinCode()).willReturn("KR7" + shortCode + "0003");
        given(item.koreanName()).willReturn(koreanName);
        given(item.sectorLarge()).willReturn("0001");
        given(item.sectorMedium()).willReturn(sectorMedium);
        given(item.sectorSmall()).willReturn("0000");
        return item;
    }

    private KosdaqItem createKosdaqItem(String shortCode, String koreanName, String sectorMedium) {
        KosdaqItem item = mock(KosdaqItem.class);
        given(item.shortCode()).willReturn(shortCode);
        given(item.isinCode()).willReturn("KR7" + shortCode + "0001");
        given(item.koreanName()).willReturn(koreanName);
        given(item.sectorLarge()).willReturn("0001");
        given(item.sectorMedium()).willReturn(sectorMedium);
        given(item.sectorSmall()).willReturn("0000");
        return item;
    }
}

package org.stockwellness.application.service.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockMasterSyncService 단위 테스트")
class StockMasterSyncServiceTest {

    @Mock
    private KisMasterClient kisMasterClient;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private MarketIndexRepository marketIndexRepository;

    @Test
    @DisplayName("신규 KOSPI 종목이면 Stock을 생성한다")
    void upsertKospi_CreatesNewStock() {
        StockMasterSyncService service = new StockMasterSyncService(kisMasterClient, stockRepository, marketIndexRepository);
        KospiItem item = kospiItem("005930", "삼성전자");
        given(marketIndexRepository.findActiveIndexMap()).willReturn(Map.of("0029", MarketIndex.of("0029", "전기전자")));
        given(stockRepository.findByTicker("005930")).willReturn(Optional.empty());

        StockMasterSyncUseCase.StockMasterSyncResult result = service.upsertKospi(
                new StockMasterSyncUseCase.KospiMasterSyncCommand(item)
        );

        assertThat(result.created()).isTrue();
        assertThat(result.stock().getTicker()).isEqualTo("005930");
        assertThat(result.stock().getName()).isEqualTo("삼성전자");
        assertThat(result.stock().getMarketType()).isEqualTo(MarketType.KOSPI);
        assertThat(result.stock().getSector().getSectorCode()).isEqualTo("0029");
        assertThat(result.stock().getSector().getSectorName()).isEqualTo("전기전자");
    }

    @Test
    @DisplayName("신규 KOSDAQ 종목이면 Stock을 생성한다")
    void upsertKosdaq_CreatesNewStock() {
        StockMasterSyncService service = new StockMasterSyncService(kisMasterClient, stockRepository, marketIndexRepository);
        KosdaqItem item = kosdaqItem("000250", "삼천당제약");
        given(marketIndexRepository.findActiveIndexMap()).willReturn(Map.of("0027", MarketIndex.of("0027", "제약")));
        given(stockRepository.findByTicker("000250")).willReturn(Optional.empty());

        StockMasterSyncUseCase.StockMasterSyncResult result = service.upsertKosdaq(
                new StockMasterSyncUseCase.KosdaqMasterSyncCommand(item)
        );

        assertThat(result.created()).isTrue();
        assertThat(result.stock().getTicker()).isEqualTo("000250");
        assertThat(result.stock().getName()).isEqualTo("삼천당제약");
        assertThat(result.stock().getMarketType()).isEqualTo(MarketType.KOSDAQ);
        assertThat(result.stock().getSector().getSectorCode()).isEqualTo("0027");
        assertThat(result.stock().getSector().getSectorName()).isEqualTo("제약");
    }

    @Test
    @DisplayName("기존 종목이면 마스터 정보로 갱신한다")
    void upsertKospi_UpdatesExistingStock() {
        StockMasterSyncService service = new StockMasterSyncService(kisMasterClient, stockRepository, marketIndexRepository);
        Stock existing = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW,
                StockSector.of("0001", "0029", null, "전기전자"), StockStatus.ACTIVE);
        KospiItem item = kospiItem("005930", "삼성전자우");
        given(marketIndexRepository.findActiveIndexMap()).willReturn(Map.of("0029", MarketIndex.of("0029", "전기전자")));
        given(stockRepository.findByTicker("005930")).willReturn(Optional.of(existing));

        StockMasterSyncUseCase.StockMasterSyncResult result = service.upsertKospi(
                new StockMasterSyncUseCase.KospiMasterSyncCommand(item)
        );

        assertThat(result.created()).isFalse();
        assertThat(result.stock()).isSameAs(existing);
        assertThat(existing.getName()).isEqualTo("삼성전자우");
        assertThat(existing.getSector().getSectorName()).isEqualTo("전기전자");
    }

    @Test
    @DisplayName("단축코드가 비어 있으면 종목 동기화를 건너뛴다")
    void upsertKospi_SkipsBlankShortCode() {
        StockMasterSyncService service = new StockMasterSyncService(kisMasterClient, stockRepository, marketIndexRepository);
        KospiItem item = mock(KospiItem.class);
        given(item.shortCode()).willReturn(" ");
        given(item.isinCode()).willReturn("KR7000000000");

        StockMasterSyncUseCase.StockMasterSyncResult result = service.upsertKospi(
                new StockMasterSyncUseCase.KospiMasterSyncCommand(item)
        );

        assertThat(result.created()).isFalse();
        assertThat(result.stock()).isNull();
        verifyNoInteractions(marketIndexRepository, stockRepository);
    }

    @Test
    @DisplayName("활성 ticker가 비어 있으면 상장폐지 처리를 건너뛴다")
    void markDelisted_SkipsEmptyActiveTickers() {
        StockMasterSyncService service = new StockMasterSyncService(kisMasterClient, stockRepository, marketIndexRepository);

        StockMasterSyncUseCase.StockDelistResult result = service.markDelisted(
                new StockMasterSyncUseCase.StockDelistCommand(MarketType.KOSPI, Set.of())
        );

        assertThat(result.marketType()).isEqualTo(MarketType.KOSPI);
        assertThat(result.delistedCount()).isZero();
        verify(stockRepository, never()).delistMissingStocks(any(), any());
    }

    @Test
    @DisplayName("활성 ticker가 있으면 누락 종목을 상장폐지 처리한다")
    void markDelisted_DelegatesRepository() {
        StockMasterSyncService service = new StockMasterSyncService(kisMasterClient, stockRepository, marketIndexRepository);
        Set<String> activeTickers = Set.of("005930", "000660");
        given(stockRepository.delistMissingStocks(MarketType.KOSPI, activeTickers)).willReturn(3);

        StockMasterSyncUseCase.StockDelistResult result = service.markDelisted(
                new StockMasterSyncUseCase.StockDelistCommand(MarketType.KOSPI, activeTickers)
        );

        assertThat(result.delistedCount()).isEqualTo(3);
        verify(stockRepository).delistMissingStocks(MarketType.KOSPI, activeTickers);
    }

    private KospiItem kospiItem(String shortCode, String koreanName) {
        KospiItem item = mock(KospiItem.class);
        given(item.shortCode()).willReturn(shortCode);
        given(item.isinCode()).willReturn("KR7" + shortCode.strip() + "0003");
        given(item.koreanName()).willReturn(koreanName);
        given(item.groupCode()).willReturn("ST");
        given(item.marketCapSize()).willReturn("1");
        given(item.sectorLarge()).willReturn("0001");
        given(item.sectorMedium()).willReturn("0029");
        given(item.sectorSmall()).willReturn("0000");
        lenient().when(item.listingDate()).thenReturn("19750611");
        given(item.parValue()).willReturn("000000000100");
        given(item.fiscalMonth()).willReturn("12");
        given(item.preferredStockType()).willReturn("0");
        given(item.clearingTrade()).willReturn("N");
        given(item.tradingHalt()).willReturn("N");
        given(item.administeredStock()).willReturn("N");
        given(item.marketWarningLevel()).willReturn("00");
        given(item.warningNotice()).willReturn("N");
        given(item.unfaithfulDisclosure()).willReturn("N");
        given(item.backdoorListing()).willReturn("N");
        given(item.shortTermOverheat()).willReturn("N");
        given(item.shortSellOverheat()).willReturn("N");
        given(item.abnormalSurge()).willReturn("N");
        return item;
    }

    private KosdaqItem kosdaqItem(String shortCode, String koreanName) {
        KosdaqItem item = mock(KosdaqItem.class);
        given(item.shortCode()).willReturn(shortCode);
        given(item.isinCode()).willReturn("KR7" + shortCode.strip() + "0001");
        given(item.koreanName()).willReturn(koreanName);
        given(item.groupCode()).willReturn("ST");
        given(item.marketCapSize()).willReturn("1");
        given(item.sectorLarge()).willReturn("0001");
        given(item.sectorMedium()).willReturn("0027");
        given(item.sectorSmall()).willReturn("0000");
        given(item.listingDate()).willReturn("20001004");
        given(item.parValue()).willReturn("000000000500");
        given(item.fiscalMonth()).willReturn("12");
        given(item.preferredStockType()).willReturn("0");
        given(item.clearingTrade()).willReturn("N");
        given(item.tradingHalt()).willReturn("N");
        given(item.administeredStock()).willReturn("N");
        given(item.marketWarningLevel()).willReturn("00");
        given(item.warningNotice()).willReturn("N");
        given(item.unfaithfulDisclosure()).willReturn("N");
        given(item.backdoorListing()).willReturn("N");
        given(item.shortTermOverheat()).willReturn("N");
        given(item.shortSellOverheat()).willReturn("N");
        given(item.abnormalSurge()).willReturn("N");
        given(item.investCaution()).willReturn("N");
        return item;
    }
}

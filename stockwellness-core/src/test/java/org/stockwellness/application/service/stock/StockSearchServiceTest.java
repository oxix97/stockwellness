package org.stockwellness.application.service.stock;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.out.stock.PopularSearchPort;
import org.stockwellness.application.port.out.stock.SearchHistoryPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockSearchService 단위 테스트")
class StockSearchServiceTest {

    @Mock
    private SearchHistoryPort searchHistoryPort;

    @Mock
    private PopularSearchPort popularSearchPort;

    @InjectMocks
    private StockSearchService stockSearchService;

    @Test
    @DisplayName("검색 기록 저장 시 TTL(30일)이 설정된다")
    void saveSearchHistory_with_ttl() {
        // Given
        Long memberId = 1L;
        String keyword = "삼성전자";
        Duration expectedTtl = Duration.ofDays(30);

        // When
        stockSearchService.saveSearchHistory(memberId, keyword);

        // Then
        verify(searchHistoryPort).save(memberId, keyword);
        verify(searchHistoryPort).setExpireTime(memberId, expectedTtl);
    }

    @Test
    @DisplayName("최근 검색어 목록을 조회한다")
    void getRecentSearches() {
        // Given
        Long memberId = 1L;
        List<String> expected = List.of("삼성전자", "Apple");
        given(searchHistoryPort.findAll(memberId)).willReturn(expected);

        // When
        List<String> result = stockSearchService.getRecentSearches(memberId);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(searchHistoryPort).findAll(memberId);
    }

    @Test
    @DisplayName("특정 검색 기록을 삭제한다")
    void removeSearchHistory() {
        // Given
        Long memberId = 1L;
        String keyword = "삼성전자";

        // When
        stockSearchService.removeSearchHistory(memberId, keyword);

        // Then
        verify(searchHistoryPort).delete(memberId, keyword);
    }

    @Test
    @DisplayName("전체 검색 기록을 삭제한다")
    void clearSearchHistory() {
        // Given
        Long memberId = 1L;

        // When
        stockSearchService.clearSearchHistory(memberId);

        // Then
        verify(searchHistoryPort).deleteAll(memberId);
    }

    @Test
    @DisplayName("인기 검색어 10개를 조회한다")
    void getPopularSearches() {
        // Given
        List<String> expected = List.of("삼성전자", "SK하이닉스");
        given(popularSearchPort.findTop10()).willReturn(expected);

        // When
        List<String> result = stockSearchService.getPopularSearches();

        // Then
        assertThat(result).isEqualTo(expected);
        verify(popularSearchPort).findTop10();
    }
}

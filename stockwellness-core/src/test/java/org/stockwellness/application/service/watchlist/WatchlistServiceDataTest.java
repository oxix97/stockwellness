package org.stockwellness.application.service.watchlist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.application.port.in.watchlist.dto.WatchlistItemListResponse;
import org.stockwellness.application.port.out.watchlist.StockDataPort;
import org.stockwellness.application.port.out.watchlist.WatchlistPort;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.watchlist.WatchlistGroup;
import org.stockwellness.domain.watchlist.WatchlistItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceDataTest {

    @InjectMocks
    private WatchlistService watchlistService;

    @Mock
    private WatchlistPort watchlistPort;
    @Mock
    private StockDataPort stockDataPort;

    @Test
    void getItems가_상세_데이터를_포함하여_반환한다() {
        // given
        Long memberId = 1L;
        Long groupId = 1L;
        Member member = Member.register("test@test.com", "tester", LoginType.GOOGLE);
        ReflectionTestUtils.setField(member, "id", memberId);
        
        WatchlistGroup group = WatchlistGroup.create(member, "테스트 그룹");
        ReflectionTestUtils.setField(group, "id", groupId);

        Stock stock = Stock.of("000660", "KR7000660001", "SK하이닉스", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 100L);

        WatchlistItem item = WatchlistItem.create(group, stock);
        
        given(watchlistPort.findGroupById(groupId)).willReturn(Optional.of(group));
        given(watchlistPort.findItemsWithStock(group)).willReturn(List.of(item));
        
        StockDataPort.StockWellnessDetail detail = new StockDataPort.StockWellnessDetail(
                "KR7000660001",
                new BigDecimal("180000"),
                new BigDecimal("2.5"),
                new BigDecimal("65.0"),
                "중립",
                "정배열 추세"
        );
        given(stockDataPort.getStockDetails(any())).willReturn(Map.of("KR7000660001", detail));

        // when
        WatchlistItemListResponse response = watchlistService.getItems(memberId, groupId);

        // then
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).currentPrice()).isEqualByComparingTo("180000");
        assertThat(response.items().get(0).rsiStatus()).isEqualTo("중립");
        assertThat(response.items().get(0).aiInsight()).isEqualTo("정배열 추세");
    }
}

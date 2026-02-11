package org.stockwellness.domain.watchlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WatchlistItemTest {

    @Test
    @DisplayName("관심 종목 아이템을 생성한다")
    void createWatchlistItem() {
        // given
        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹1");
        Stock stock = Stock.create("KR7005930003", "삼성전자", "005930", MarketType.KOSPI, 1000L, "1234", "삼성");

        // when
        WatchlistItem item = WatchlistItem.create(group, stock);

        // then
        assertThat(item.getGroup()).isEqualTo(group);
        assertThat(item.getStock()).isEqualTo(stock);
        assertThat(item.getMarketType()).isEqualTo(MarketType.KOSPI); // Stock에서 가져옴
        assertThat(item.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("아이템 삭제 시 deletedAt이 설정된다")
    void deleteWatchlistItem() {
        // given
        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹1");
        Stock stock = Stock.create("KR7005930003", "삼성전자", "005930", MarketType.KOSPI, 1000L, "1234", "삼성");
        WatchlistItem item = WatchlistItem.create(group, stock);

        // when
        item.delete();

        // then
        assertThat(item.getDeletedAt()).isNotNull();
        assertThat(item.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
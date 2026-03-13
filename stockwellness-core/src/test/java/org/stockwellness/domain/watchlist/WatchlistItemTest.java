package org.stockwellness.domain.watchlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.*;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WatchlistItemTest {

    @Test
    @DisplayName("관심 종목 아이템을 생성한다")
    void createWatchlistItem() {
        // given
        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹1");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);

        // when
        WatchlistItem item = WatchlistItem.create(group, stock, "메모");

        // then
        assertThat(item.getGroup()).isEqualTo(group);
        assertThat(item.getStock()).isEqualTo(stock);
        assertThat(item.getTicker()).isEqualTo("005930");
        assertThat(item.getMarketType()).isEqualTo(MarketType.KOSPI);
        assertThat(item.getNote()).isEqualTo("메모");
        assertThat(item.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("아이템 삭제 시 deletedAt이 설정된다")
    void deleteWatchlistItem() {
        // given
        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹1");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        WatchlistItem item = WatchlistItem.create(group, stock);

        // when
        item.delete();

        // then
        assertThat(item.getDeletedAt()).isNotNull();
        assertThat(item.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("메모를 수정한다")
    void updateNote() {
        // given
        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹1");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        WatchlistItem item = WatchlistItem.create(group, stock, "기존 메모");

        // when
        item.updateNote("수정된 메모");

        // then
        assertThat(item.getNote()).isEqualTo("수정된 메모");
    }

    @Test
    @DisplayName("메모는 200자를 초과할 수 없다")
    void validateNoteLength() {
        // given
        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹1");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        String longNote = "A".repeat(201);

        // when & then
        assertThatThrownBy(() -> WatchlistItem.create(group, stock, longNote))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.WATCHLIST_NOTE_LIMIT_EXCEEDED.getMessage());
    }
}

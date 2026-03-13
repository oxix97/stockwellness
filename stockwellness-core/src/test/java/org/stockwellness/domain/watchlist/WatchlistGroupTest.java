package org.stockwellness.domain.watchlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.*;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WatchlistGroupTest {

    @Test
    @DisplayName("관심 종목 그룹을 생성한다")
    void createWatchlistGroup() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        String name = "내 관심 그룹";

        // when
        WatchlistGroup group = WatchlistGroup.create(member, name);

        // then
        assertThat(group.getMember()).isEqualTo(member);
        assertThat(group.getName()).isEqualTo(name);
        assertThat(group.getDeletedAt()).isNull();
        assertThat(group.getItems()).isEmpty();
    }

    @Test
    @DisplayName("그룹 이름을 변경한다")
    void renameWatchlistGroup() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "기존 이름");
        String newName = "새 이름";

        // when
        group.rename(newName);

        // then
        assertThat(group.getName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("그룹을 삭제하면 아이템도 Soft Delete 된다")
    void deleteWatchlistGroup() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "삭제할 그룹");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        group.addItem(stock);
        WatchlistItem item = group.getItems().get(0);

        // when
        group.delete();

        // then
        assertThat(group.getDeletedAt()).isNotNull();
        assertThat(item.getDeletedAt()).isNotNull();
        assertThat(group.getItems()).isEmpty();
    }

    @Test
    @DisplayName("그룹에 종목을 추가한다")
    void addItem() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);

        // when
        group.addItem(stock, "메모");

        // then
        assertThat(group.getItems()).hasSize(1);
        assertThat(group.getItems().get(0).getStock()).isEqualTo(stock);
        assertThat(group.getItems().get(0).getNote()).isEqualTo("메모");
    }

    @Test
    @DisplayName("그룹 내 중복된 종목을 추가할 수 없다 (티커 기준)")
    void addItemDuplicate() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        group.addItem(stock);

        // when & then
        assertThatThrownBy(() -> group.addItem(stock))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_WATCHLIST_ITEM.getMessage());
    }

    @Test
    @DisplayName("그룹에서 종목의 메모를 수정한다")
    void updateItemNote() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        group.addItem(stock, "기존 메모");

        // when
        group.updateItemNote("005930", "수정 메모");

        // then
        assertThat(group.getItems().get(0).getNote()).isEqualTo("수정 메모");
    }

    @Test
    @DisplayName("그룹에서 종목을 제거한다 (티커 기준)")
    void removeItem() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        group.addItem(stock);

        // when
        group.removeItem("005930");

        // then
        assertThat(group.getItems()).isEmpty();
    }

    @Test
    @DisplayName("그룹 내 아이템은 최대 50개까지만 추가할 수 있다")
    void addItemLimit() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");

        for (int i = 0; i < 50; i++) {
            Stock stock = Stock.of("TICKER" + i, "ISIN" + i, "Stock" + i, MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
            group.addItem(stock);
        }

        // when & then
        Stock newStock = Stock.of("NEW", "NEW_ISIN", "New", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        assertThatThrownBy(() -> group.addItem(newStock))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.WATCHLIST_ITEM_LIMIT_EXCEEDED.getMessage());
    }
}

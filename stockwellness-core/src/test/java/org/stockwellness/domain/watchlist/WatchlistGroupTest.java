package org.stockwellness.domain.watchlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.*;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;
import org.springframework.test.util.ReflectionTestUtils;
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
        assertThat(group.getItems()).containsExactly(item);
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
    @DisplayName("같은 종목은 활성 관심종목으로 중복 추가할 수 없다")
    void addItemDuplicate() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 1L);
        Stock sameStock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(sameStock, "id", 1L);
        group.addItem(stock);

        // when & then
        assertThatThrownBy(() -> group.addItem(sameStock))
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
        group.updateItemNote(stock, "수정 메모");

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
        WatchlistItem item = group.getItems().getFirst();
        group.removeItem(stock);

        // then
        assertThat(group.getItems()).containsExactly(item);
        assertThat(item.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("삭제한 종목을 다시 추가하면 기존 항목을 복원한다")
    void addItemRestoresDeletedItem() {
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
        Stock stock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(stock, "id", 1L);
        group.addItem(stock, "old note");
        WatchlistItem original = group.getItems().getFirst();
        Stock sameStock = Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(sameStock, "id", 1L);
        group.removeItem(sameStock);

        group.addItem(sameStock, "new note");

        assertThat(group.getItems()).containsExactly(original);
        assertThat(original.getDeletedAt()).isNull();
        assertThat(original.getNote()).isEqualTo("new note");
    }

    @Test
    @DisplayName("같은 티커라도 다른 종목 식별자는 그룹에 추가할 수 있다")
    void sameTickerDifferentStocksAreDistinct() {
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
        Stock kospiStock = Stock.of("DUP", "KR0000000001", "KOSPI 종목", MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        Stock nasdaqStock = Stock.of("DUP", null, "NASDAQ 종목", MarketType.NASDAQ, Currency.USD, null, StockStatus.ACTIVE);
        ReflectionTestUtils.setField(kospiStock, "id", 1L);
        ReflectionTestUtils.setField(nasdaqStock, "id", 2L);

        group.addItem(kospiStock);
        group.addItem(nasdaqStock);

        assertThat(group.getItems()).hasSize(2);
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

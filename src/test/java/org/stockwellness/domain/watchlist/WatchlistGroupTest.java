package org.stockwellness.domain.watchlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

import java.time.LocalDateTime;

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
        Stock stock = Stock.create("KR7005930003", "Samsung", "005930", MarketType.KOSPI, 100L, "123", "Corp");
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
        Stock stock = Stock.create("KR7005930003", "Samsung", "005930", MarketType.KOSPI, 100L, "123", "Corp");

        // when
        group.addItem(stock);

        // then
        assertThat(group.getItems()).hasSize(1);
        assertThat(group.getItems().get(0).getStock()).isEqualTo(stock);
    }

    @Test
    @DisplayName("그룹 내 중복된 종목을 추가할 수 없다")
    void addItemDuplicate() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
        Stock stock = Stock.create("KR7005930003", "Samsung", "005930", MarketType.KOSPI, 100L, "123", "Corp");
        group.addItem(stock);
        
        // when & then
        assertThatThrownBy(() -> group.addItem(stock))
                .isInstanceOf(org.stockwellness.global.error.exception.BusinessException.class)
                .hasMessage("이미 그룹에 등록된 종목입니다.");
    }
    
    @Test
    @DisplayName("그룹에서 종목을 제거한다")
    void removeItem() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
        Stock stock = Stock.create("KR7005930003", "Samsung", "005930", MarketType.KOSPI, 100L, "123", "Corp");
        group.addItem(stock);
        
        // when
        group.removeItem(stock.getIsinCode());
        
        // then
        assertThat(group.getItems()).isEmpty();
    }

    @Test
    @DisplayName("그룹 내 아이템은 최대 50개까지만 추가할 수 있다")
    void addItemLimit() {
        // given
        Member member = Member.register("test@email.com", "tester", LoginType.KAKAO);
        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
        
        // 50개 채우기
        for (int i = 0; i < 50; i++) {
            Stock stock = Stock.create("STOCK" + i, "Stock" + i, "000" + i, MarketType.KOSPI, 100L, "123", "Corp");
            group.addItem(stock);
        }

        // when & then
        Stock newStock = Stock.create("NEW", "New", "999", MarketType.KOSPI, 100L, "123", "Corp");
        assertThatThrownBy(() -> group.addItem(newStock))
                .isInstanceOf(org.stockwellness.global.error.exception.BusinessException.class)
                .hasMessage("관심 종목은 한 그룹당 최대 50개까지 추가할 수 있습니다.");
    }
}
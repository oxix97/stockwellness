package org.stockwellness.adapter.out.persistence.watchlist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.config.TestJpaConfig;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.watchlist.WatchlistGroup;
import org.stockwellness.domain.watchlist.WatchlistItem;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, TestJpaConfig.class})
class WatchlistItemRepositoryTest {

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private WatchlistGroupRepository watchlistGroupRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StockRepository stockRepository;

    @Test
    @DisplayName("그룹으로 아이템 목록을 조회한다 (삭제되지 않은 아이템만)")
    void findAllByGroupAndDeletedAtIsNull() {
        // given
        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
        memberRepository.save(member);

        WatchlistGroup group = WatchlistGroup.create(member, "그룹1");
        watchlistGroupRepository.save(group);

        Stock stock1 = Stock.create("STOCK1", "종목1", "000001", MarketType.KOSPI, 100L, "111", "CORP1");
        Stock stock2 = Stock.create("STOCK2", "종목2", "000002", MarketType.KOSPI, 100L, "222", "CORP2");
        Stock stock3 = Stock.create("STOCK3", "종목3", "000003", MarketType.KOSPI, 100L, "333", "CORP3");
        stockRepository.saveAll(List.of(stock1, stock2, stock3));

        WatchlistItem item1 = WatchlistItem.create(group, stock1);
        WatchlistItem item2 = WatchlistItem.create(group, stock2);
        WatchlistItem deletedItem = WatchlistItem.create(group, stock3);
        deletedItem.delete();

        watchlistItemRepository.saveAll(List.of(item1, item2, deletedItem));

        // when
        List<WatchlistItem> items = watchlistItemRepository.findAllByGroupAndDeletedAtIsNull(group);

        // then
        assertThat(items).hasSize(2)
                .extracting("stock.isinCode")
                .containsExactlyInAnyOrder("STOCK1", "STOCK2");
    }
}
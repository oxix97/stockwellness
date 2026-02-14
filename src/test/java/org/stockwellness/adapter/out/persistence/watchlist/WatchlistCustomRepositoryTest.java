//package org.stockwellness.adapter.out.persistence.watchlist;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.context.annotation.Import;
//import org.stockwellness.adapter.out.persistence.member.MemberRepository;
//import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
//import org.stockwellness.application.port.out.watchlist.dto.WatchlistGroupWithCount;
//import org.stockwellness.config.QueryDslConfig;
//import org.stockwellness.config.TestJpaConfig;
//import org.stockwellness.domain.member.LoginType;
//import org.stockwellness.domain.member.Member;
//import org.stockwellness.domain.stock.MarketType;
//import org.stockwellness.domain.stock.Stock;
//import org.stockwellness.domain.watchlist.WatchlistGroup;
//import org.stockwellness.domain.watchlist.WatchlistItem;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//@Import({TestJpaConfig.class, QueryDslConfig.class})
//class WatchlistCustomRepositoryTest {
//
//    @Autowired
//    private WatchlistGroupRepository watchlistGroupRepository;
//
//    @Autowired
//    private WatchlistItemRepository watchlistItemRepository;
//
//    @Autowired
//    private MemberRepository memberRepository;
//
//    @Autowired
//    private StockRepository stockRepository;
//
//    @Test
//    @DisplayName("멤버의 관심 그룹 목록과 각 그룹의 아이템 개수를 조회한다")
//    void findGroupsWithItemCount() {
//        // given
//        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
//        memberRepository.save(member);
//
//        WatchlistGroup group1 = WatchlistGroup.create(member, "그룹 1");
//        WatchlistGroup group2 = WatchlistGroup.create(member, "그룹 2");
//        watchlistGroupRepository.saveAll(List.of(group1, group2));
//
//        Stock stock1 = Stock.of("S1", "N1", "T1", MarketType.KOSPI, 1L, "C1", "CN1");
//        Stock stock2 = Stock.of("S2", "N2", "T2", MarketType.KOSPI, 1L, "C2", "CN2");
//        stockRepository.saveAll(List.of(stock1, stock2));
//
//        watchlistItemRepository.save(WatchlistItem.create(group1, stock1));
//        watchlistItemRepository.save(WatchlistItem.create(group1, stock2));
//        watchlistItemRepository.save(WatchlistItem.create(group2, stock1));
//
//        // when
//        List<WatchlistGroupWithCount> results = watchlistGroupRepository.findGroupsWithItemCount(member.getId());
//
//        // then
//        assertThat(results).hasSize(2);
//        assertThat(results).anyMatch(r -> r.group().getName().equals("그룹 1") && r.itemCount() == 2);
//        assertThat(results).anyMatch(r -> r.group().getName().equals("그룹 2") && r.itemCount() == 1);
//    }
//
//    @Test
//    @DisplayName("그룹 내 아이템 목록을 Stock 정보와 함께 조회한다")
//    void findItemsWithStock() {
//        // given
//        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
//        memberRepository.save(member);
//
//        WatchlistGroup group = WatchlistGroup.create(member, "그룹 1");
//        watchlistGroupRepository.save(group);
//
//        Stock stock = Stock.of("S1", "삼성전자", "005930", MarketType.KOSPI, 1L, "C1", "CN1");
//        stockRepository.save(stock);
//
//        watchlistItemRepository.save(WatchlistItem.create(group, stock));
//
//        // when
//        List<WatchlistItem> items = watchlistGroupRepository.findItemsWithStock(group);
//
//        // then
//        assertThat(items).hasSize(1);
//        assertThat(items.get(0).getStock().getName()).isEqualTo("삼성전자");
//    }
//}

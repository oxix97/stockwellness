//package org.stockwellness.application.service.watchlist;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.stockwellness.adapter.in.web.watchlist.dto.WatchlistGroupResponse;
//import org.stockwellness.application.port.out.member.LoadMemberPort;
//import org.stockwellness.application.port.out.stock.StockPort;
//import org.stockwellness.application.port.out.watchlist.StockDataPort;
//import org.stockwellness.application.port.out.watchlist.WatchlistPort;
//import org.stockwellness.application.port.out.watchlist.dto.WatchlistGroupWithCount;
//import org.stockwellness.domain.member.LoginType;
//import org.stockwellness.domain.member.Member;
//import org.stockwellness.domain.shared.AbstractEntity;
//import org.stockwellness.domain.stock.MarketType;
//import org.stockwellness.domain.stock.Stock;
//import org.stockwellness.domain.watchlist.WatchlistGroup;
//
//import java.lang.reflect.Field;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//class WatchlistServiceTest {
//
//    @Mock
//    private WatchlistPort watchlistPort;
//
//    @Mock
//    private LoadMemberPort loadMemberPort;
//
//    @Mock
//    private StockPort stockPort;
//
//    @Mock
//    private StockDataPort stockDataPort;
//
//    @InjectMocks
//    private WatchlistService watchlistService;
//
//    @Test
//    @DisplayName("관심 그룹을 생성한다")
//    void createGroup() {
//        // given
//        Long memberId = 1L;
//        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
//        given(watchlistPort.countGroupsByMemberId(memberId)).willReturn(0L);
//        given(loadMemberPort.loadMember(memberId)).willReturn(Optional.of(member));
//        given(watchlistPort.saveGroup(any(WatchlistGroup.class))).willAnswer(invocation -> invocation.getArgument(0));
//
//        // when
//        watchlistService.createGroup(memberId, "새 그룹");
//
//        // then
//        verify(watchlistPort).saveGroup(any(WatchlistGroup.class));
//    }
//
//    @Test
//    @DisplayName("관심 그룹 이름을 수정한다")
//    void updateGroupName() {
//        // given
//        Long memberId = 1L;
//        Long groupId = 10L;
//        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
//        setEntityId(member, memberId);
//        WatchlistGroup group = WatchlistGroup.create(member, "기존 이름");
//
//        given(watchlistPort.findGroupById(groupId)).willReturn(Optional.of(group));
//
//        // when
//        watchlistService.updateGroupName(memberId, groupId, "새 이름");
//
//        // then
//        assertThat(group.getName()).isEqualTo("새 이름");
//    }
//
//    @Test
//    @DisplayName("관심 그룹 목록을 조회한다")
//    void getGroups() {
//        // given
//        Long memberId = 1L;
//        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
//
//        WatchlistGroup group = WatchlistGroup.create(member, "그룹1");
//        setEntityId(group, 10L);
//
//        given(watchlistPort.findGroupsWithItemCount(memberId)).willReturn(List.of(
//                new WatchlistGroupWithCount(group, 5L)
//        ));
//
//        // when
//        List<WatchlistGroupResponse> results = watchlistService.getGroups(memberId);
//
//        // then
//        assertThat(results).hasSize(1);
//        assertThat(results.get(0).name()).isEqualTo("그룹1");
//        assertThat(results.get(0).itemCount()).isEqualTo(5L);
//    }
//
//    @Test
//    @DisplayName("관심 그룹에 종목을 추가한다")
//    void addItem() {
//        // given
//        Long memberId = 1L;
//        Long groupId = 10L;
//        String isinCode = "KR7005930003";
//
//        Member member = Member.register("test@test.com", "tester", LoginType.KAKAO);
//        setEntityId(member, memberId);
//        WatchlistGroup group = WatchlistGroup.create(member, "그룹");
//        Stock stock = Stock.of(isinCode, "삼성", "005930", MarketType.KOSPI, 1L, "C", "CN");
//
//        given(watchlistPort.findGroupById(groupId)).willReturn(Optional.of(group));
//        given(stockPort.loadStockByTicker(isinCode)).willReturn(Optional.of(stock));
//
//        // when
//        watchlistService.addItem(memberId, groupId, isinCode);
//
//        // then
//        assertThat(group.getItems()).hasSize(1);
//        assertThat(group.getItems().get(0).getStock()).isEqualTo(stock);
//    }
//
//    private void setEntityId(Object entity, Long id) {
//        try {
//            Field idField = AbstractEntity.class.getDeclaredField("id");
//            idField.setAccessible(true);
//            idField.set(entity, id);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
package org.stockwellness.application.service.watchlist;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.in.web.watchlist.dto.WatchlistGroupResponse;
import org.stockwellness.adapter.in.web.watchlist.dto.WatchlistItemListResponse;
import org.stockwellness.adapter.in.web.watchlist.dto.WatchlistItemListResponse.WatchlistItemDetail;
import org.stockwellness.application.port.in.watchlist.WatchlistUseCase;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.application.port.out.stock.LoadStockPort;
import org.stockwellness.application.port.out.watchlist.StockDataPort;
import org.stockwellness.application.port.out.watchlist.WatchlistPort;
import org.stockwellness.application.port.out.watchlist.dto.WatchlistGroupWithCount;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.watchlist.WatchlistGroup;
import org.stockwellness.domain.watchlist.WatchlistItem;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchlistService implements WatchlistUseCase {

    private final WatchlistPort watchlistPort;
    private final LoadMemberPort loadMemberPort;
    private final LoadStockPort loadStockPort;
    private final StockDataPort stockDataPort;

    @Override
    public Long createGroup(Long memberId, String name) {
        long currentCount = watchlistPort.countGroupsByMemberId(memberId);
        WatchlistGroup.validateGroupCount(currentCount);

        Member member = loadMemberPort.loadMember(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        WatchlistGroup group = WatchlistGroup.create(member, name);
        return watchlistPort.saveGroup(group).getId();
    }

    @Override
    public void createDefaultGroup(Member member) {
        WatchlistGroup defaultGroup = WatchlistGroup.createDefault(member);
        watchlistPort.saveGroup(defaultGroup);
    }

    @Override
    public void updateGroupName(Long memberId, Long groupId, String newName) {
        WatchlistGroup group = getGroupAndCheckOwnership(memberId, groupId);
        group.rename(newName);
    }

    @Override
    public void deleteGroup(Long memberId, Long groupId) {
        WatchlistGroup group = getGroupAndCheckOwnership(memberId, groupId);
        group.delete();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WatchlistGroupResponse> getGroups(Long memberId) {
        List<WatchlistGroupWithCount> groups = watchlistPort.findGroupsWithItemCount(memberId);
        return groups.stream()
                .map(g -> new WatchlistGroupResponse(
                        g.group().getId(),
                        g.group().getName(),
                        g.itemCount()))
                .toList();
    }

    @Override
    public void addItem(Long memberId, Long groupId, String isinCode) {
        WatchlistGroup group = getGroupAndCheckOwnership(memberId, groupId);
        Stock stock = loadStockPort.loadStockByTicker(isinCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        group.addItem(stock);
    }

    @Override
    public void removeItem(Long memberId, Long groupId, String isinCode) {
        WatchlistGroup group = getGroupAndCheckOwnership(memberId, groupId);
        group.removeItem(isinCode);
    }

    @Override
    @Transactional(readOnly = true)
    public WatchlistItemListResponse getItems(Long memberId, Long groupId) {
        WatchlistGroup group = getGroupAndCheckOwnership(memberId, groupId);
        List<WatchlistItem> items = watchlistPort.findItemsWithStock(group);

        return new WatchlistItemListResponse(group.getName(), toItemDetails(items));
    }

    private List<WatchlistItemDetail> toItemDetails(List<WatchlistItem> items) {
        List<String> isinCodes = items.stream()
                .map(item -> item.getStock().getStandardCode())
                .toList();

        Map<String, StockDataPort.StockWellnessDetail> details = stockDataPort.getStockDetails(isinCodes);

        return items.stream()
                .map(item -> WatchlistItemDetail.of(item, details.get(item.getStock().getStandardCode())))
                .toList();
    }

    private WatchlistGroup getGroupAndCheckOwnership(Long memberId, Long groupId) {
        WatchlistGroup group = watchlistPort.findGroupById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!group.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return group;
    }
}

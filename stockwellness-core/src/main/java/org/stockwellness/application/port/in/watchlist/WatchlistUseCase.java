package org.stockwellness.application.port.in.watchlist;

import org.stockwellness.application.port.in.watchlist.dto.WatchlistGroupResponse;
import org.stockwellness.application.port.in.watchlist.dto.WatchlistItemListResponse;
import org.stockwellness.domain.member.Member;

import java.util.List;

public interface WatchlistUseCase {
    Long createGroup(Long memberId, String name);
    void createDefaultGroup(Member member);
    void updateGroupName(Long memberId, Long groupId, String newName);
    void deleteGroup(Long memberId, Long groupId);
    List<WatchlistGroupResponse> getGroups(Long memberId);

    void addItem(Long memberId, Long groupId, String ticker, String note);
    void removeItem(Long memberId, Long groupId, String ticker);
    void updateItemNote(Long memberId, Long groupId, String ticker, String note);
    WatchlistItemListResponse getItems(Long memberId, Long groupId);
}

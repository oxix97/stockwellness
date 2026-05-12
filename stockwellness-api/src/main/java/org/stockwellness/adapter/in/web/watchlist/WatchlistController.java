package org.stockwellness.adapter.in.web.watchlist;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.watchlist.dto.AddWatchlistItemRequest;
import org.stockwellness.adapter.in.web.watchlist.dto.CreateWatchlistGroupRequest;
import org.stockwellness.adapter.in.web.watchlist.dto.UpdateWatchlistItemNoteRequest;
import org.stockwellness.application.port.in.watchlist.WatchlistUseCase;
import org.stockwellness.application.port.in.watchlist.dto.WatchlistGroupResponse;
import org.stockwellness.application.port.in.watchlist.dto.WatchlistItemListResponse;
import org.stockwellness.global.common.response.ApiResponse;
import org.stockwellness.global.common.response.SuccessCode;
import org.stockwellness.global.security.MemberPrincipal;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistUseCase watchlistUseCase;

    @PostMapping("/groups")
    public ApiResponse<Long> createGroup(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @RequestBody @Valid CreateWatchlistGroupRequest request) {
        Long groupId = watchlistUseCase.createGroup(memberPrincipal.id(), request.name());
        return ApiResponse.success(SuccessCode.CREATED, groupId);
    }

    @GetMapping("/groups")
    public ApiResponse<List<WatchlistGroupResponse>> getGroups(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal) {
        List<WatchlistGroupResponse> response = watchlistUseCase.getGroups(memberPrincipal.id());
        return ApiResponse.success(response);
    }

    @PatchMapping("/groups/{groupId}")
    public ApiResponse<Void> updateGroupName(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @RequestBody @Valid CreateWatchlistGroupRequest request) {
        watchlistUseCase.updateGroupName(memberPrincipal.id(), groupId, request.name());
        return ApiResponse.success();
    }

    @DeleteMapping("/groups/{groupId}")
    public ApiResponse<Void> deleteGroup(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId) {
        watchlistUseCase.deleteGroup(memberPrincipal.id(), groupId);
        return ApiResponse.success();
    }

    @PostMapping("/groups/{groupId}/items")
    public ApiResponse<Void> addItem(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @RequestBody @Valid AddWatchlistItemRequest request) {
        watchlistUseCase.addItem(memberPrincipal.id(), groupId, request.ticker(), request.note());
        return ApiResponse.success(SuccessCode.CREATED, null);
    }

    @DeleteMapping("/groups/{groupId}/items/{ticker}")
    public ApiResponse<Void> removeItem(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @PathVariable String ticker) {
        watchlistUseCase.removeItem(memberPrincipal.id(), groupId, ticker);
        return ApiResponse.success();
    }

    @PatchMapping("/groups/{groupId}/items/{ticker}/note")
    public ApiResponse<Void> updateItemNote(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @PathVariable String ticker,
            @RequestBody @Valid UpdateWatchlistItemNoteRequest request) {
        watchlistUseCase.updateItemNote(memberPrincipal.id(), groupId, ticker, request.note());
        return ApiResponse.success();
    }

    @GetMapping("/groups/{groupId}/items")
    public ApiResponse<WatchlistItemListResponse> getItems(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId) {
        WatchlistItemListResponse response = watchlistUseCase.getItems(memberPrincipal.id(), groupId);
        return ApiResponse.success(response);
    }
}

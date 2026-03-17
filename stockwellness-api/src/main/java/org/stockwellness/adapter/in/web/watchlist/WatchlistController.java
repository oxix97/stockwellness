package org.stockwellness.adapter.in.web.watchlist;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.watchlist.dto.AddWatchlistItemRequest;
import org.stockwellness.adapter.in.web.watchlist.dto.CreateWatchlistGroupRequest;
import org.stockwellness.adapter.in.web.watchlist.dto.UpdateWatchlistItemNoteRequest;
import org.stockwellness.application.port.in.watchlist.dto.WatchlistGroupResponse;
import org.stockwellness.application.port.in.watchlist.dto.WatchlistItemListResponse;
import org.stockwellness.application.port.in.watchlist.WatchlistUseCase;
import org.stockwellness.global.common.response.ApiResponse;
import org.stockwellness.global.common.response.SuccessCode;
import org.stockwellness.global.security.MemberPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistUseCase watchlistUseCase;

    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<Long>> createGroup(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @RequestBody @Valid CreateWatchlistGroupRequest request) {
        Long groupId = watchlistUseCase.createGroup(memberPrincipal.id(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SuccessCode.CREATED, groupId));
    }

    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<WatchlistGroupResponse>>> getGroups(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal) {
        List<WatchlistGroupResponse> response = watchlistUseCase.getGroups(memberPrincipal.id());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<Void>> updateGroupName(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @RequestBody @Valid CreateWatchlistGroupRequest request) {
        watchlistUseCase.updateGroupName(memberPrincipal.id(), groupId, request.name());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId) {
        watchlistUseCase.deleteGroup(memberPrincipal.id(), groupId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/groups/{groupId}/items")
    public ResponseEntity<ApiResponse<Void>> addItem(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @RequestBody @Valid AddWatchlistItemRequest request) {
        watchlistUseCase.addItem(memberPrincipal.id(), groupId, request.ticker(), request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SuccessCode.CREATED, null));
    }

    @DeleteMapping("/groups/{groupId}/items/{ticker}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @PathVariable String ticker) {
        watchlistUseCase.removeItem(memberPrincipal.id(), groupId, ticker);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/groups/{groupId}/items/{ticker}/note")
    public ResponseEntity<ApiResponse<Void>> updateItemNote(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @PathVariable String ticker,
            @RequestBody @Valid UpdateWatchlistItemNoteRequest request) {
        watchlistUseCase.updateItemNote(memberPrincipal.id(), groupId, ticker, request.note());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/groups/{groupId}/items")
    public ResponseEntity<ApiResponse<WatchlistItemListResponse>> getItems(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId) {
        WatchlistItemListResponse response = watchlistUseCase.getItems(memberPrincipal.id(), groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

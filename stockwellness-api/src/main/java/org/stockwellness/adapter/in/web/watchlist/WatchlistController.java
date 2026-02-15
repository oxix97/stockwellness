package org.stockwellness.adapter.in.web.watchlist;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.stockwellness.adapter.in.web.watchlist.dto.AddWatchlistItemRequest;
import org.stockwellness.adapter.in.web.watchlist.dto.CreateWatchlistGroupRequest;
import org.stockwellness.application.port.in.watchlist.dto.WatchlistGroupResponse;
import org.stockwellness.application.port.in.watchlist.dto.WatchlistItemListResponse;
import org.stockwellness.application.port.in.watchlist.WatchlistUseCase;
import org.stockwellness.global.security.MemberPrincipal;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistUseCase watchlistUseCase;

    @PostMapping("/groups")
    public ResponseEntity<Void> createGroup(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @RequestBody @Valid CreateWatchlistGroupRequest request) {

        Long groupId = watchlistUseCase.createGroup(memberPrincipal.id(), request.name());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(groupId)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/groups")
    public ResponseEntity<List<WatchlistGroupResponse>> getGroups(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal) {

        List<WatchlistGroupResponse> response = watchlistUseCase.getGroups(memberPrincipal.id());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/groups/{groupId}")
    public ResponseEntity<Void> updateGroupName(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @RequestBody @Valid CreateWatchlistGroupRequest request) {

        watchlistUseCase.updateGroupName(memberPrincipal.id(), groupId, request.name());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId) {

        watchlistUseCase.deleteGroup(memberPrincipal.id(), groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/groups/{groupId}/items")
    public ResponseEntity<Void> addItem(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @RequestBody @Valid AddWatchlistItemRequest request) {

        watchlistUseCase.addItem(memberPrincipal.id(), groupId, request.isinCode());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/groups/{groupId}/items/{isinCode}")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId,
            @PathVariable String isinCode) {

        watchlistUseCase.removeItem(memberPrincipal.id(), groupId, isinCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups/{groupId}/items")
    public ResponseEntity<WatchlistItemListResponse> getItems(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long groupId) {

        WatchlistItemListResponse response = watchlistUseCase.getItems(memberPrincipal.id(), groupId);
        return ResponseEntity.ok(response);
    }
}
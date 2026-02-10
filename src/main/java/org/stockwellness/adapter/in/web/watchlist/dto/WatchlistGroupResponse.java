package org.stockwellness.adapter.in.web.watchlist.dto;

public record WatchlistGroupResponse(
        Long id,
        String name,
        long itemCount
) {}

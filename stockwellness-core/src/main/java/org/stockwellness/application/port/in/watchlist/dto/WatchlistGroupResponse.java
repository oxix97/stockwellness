package org.stockwellness.application.port.in.watchlist.dto;

public record WatchlistGroupResponse(
        Long id,
        String name,
        long itemCount
) {}

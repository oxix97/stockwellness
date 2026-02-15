package org.stockwellness.application.port.out.watchlist.dto;

import org.stockwellness.domain.watchlist.WatchlistGroup;

public record WatchlistGroupWithCount(
        WatchlistGroup group,
        long itemCount
) {
}

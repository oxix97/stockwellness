package org.stockwellness.adapter.out.persistence.watchlist.dto;

import org.stockwellness.domain.watchlist.WatchlistGroup;

public record WatchlistGroupResponse(
        WatchlistGroup group,
        long itemCount
) {
}

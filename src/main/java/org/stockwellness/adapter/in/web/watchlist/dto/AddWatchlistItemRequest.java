package org.stockwellness.adapter.in.web.watchlist.dto;

import jakarta.validation.constraints.NotBlank;

public record AddWatchlistItemRequest(
        @NotBlank(message = "ISIN 코드는 필수입니다.")
        String isinCode
) {}
